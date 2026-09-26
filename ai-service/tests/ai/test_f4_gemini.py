"""
AeroSentinel - Phase 4 Gemini Intelligence Test Suite
File: tests/ai/test_f4_gemini.py
Fulfills Section 9 (Privacy/PII Guard), Section 10 & 20 (Mandatory 5 Prompts),
Section 34 (F4 Testing), Section 35 (Evaluation), and Section 36 (Grounding).
"""

from pathlib import Path
import pytest
from PIL import Image

from app.schemas.gemini_contracts import (
    StructuredEventExplanation,
    CitizenVisionAnalysis,
    VisualIndicators,
    EvidenceSummaryOutput,
    CategorizedEvidence,
)
from app.services.grounding_guard import GroundingValidator
from app.services.vision_service import analyze_citizen_image
from app.services.gemini_pipeline import AeroSentinelGeminiPipeline
from app.services.privacy_guard import PrivacyGuard
from app.prompts.gemini_prompts import (
    PROMPT_VERSIONS,
    VISION_USER_PROMPT,
    EXPLANATION_USER_PROMPT_TEMPLATE,
    EVIDENCE_SUMMARY_USER_PROMPT,
    UNCERTAINTY_USER_PROMPT,
    CONFLICTING_EVIDENCE_USER_PROMPT,
)


@pytest.fixture
def mock_f3_payload():
    return {
        "timestamp": "2026-09-25T16:00:00Z",
        "h3_cell_id": "886196944dfffff",
        "model_version": "v1.0.0",
        "hotspot": {
            "calibrated_hotspot_probability": 0.85,
            "operational_threshold": 0.20,
            "is_hotspot": True,
            "governance_claim": "Associated statistical observations only."
        },
        "forecast": {
            "pm25_t_plus_1h": 142.5,
            "pm25_t_plus_3h": 158.0,
            "pm25_t_plus_6h": 164.2,
            "uncertainty_lower_bound_p10": 136.0,
            "uncertainty_upper_bound_p90": 172.5
        },
        "confidence": {
            "overall_confidence": 0.78,
            "model_margin_confidence": 0.82,
            "data_completeness_score": 0.90,
            "spatial_coverage_confidence": 0.65
        },
        "evidence": [
            {
                "category": "DIRECT_OBSERVATION",
                "source": "CPCB_CAAQMS_GROUND_SENSOR",
                "metric": "pm25_concentration",
                "value": 145.0,
                "attribution_note": "Ground PM2.5 measurement (145.0 ug/m3) exceeds threshold."
            },
            {
                "category": "REMOTE_SENSING",
                "source": "NASA_FIRMS_VIIRS",
                "metric": "fire_upwind_alignment_score",
                "value": 7.8,
                "attribution_note": "3 thermal anomalies detected upwind within 25km."
            }
        ]
    }


def test_mandatory_prompt_templates_defined_and_versioned():
    """Verifies that all 5 prompt templates required by Section 20 exist and are versioned."""
    assert "vision_analysis" in PROMPT_VERSIONS
    assert "structured_event_explanation" in PROMPT_VERSIONS
    assert "evidence_summary" in PROMPT_VERSIONS
    assert "uncertainty_explanation" in PROMPT_VERSIONS
    assert "conflicting_evidence" in PROMPT_VERSIONS

    assert len(VISION_USER_PROMPT) > 50
    assert len(EXPLANATION_USER_PROMPT_TEMPLATE) > 50
    assert len(EVIDENCE_SUMMARY_USER_PROMPT) > 50
    assert len(UNCERTAINTY_USER_PROMPT) > 50
    assert len(CONFLICTING_EVIDENCE_USER_PROMPT) > 50


def test_privacy_guard_redacts_phone_and_plates():
    """Verifies Section 9: PII scrubbing for Indian phone numbers, emails, and vehicle plates."""
    raw_text = "Citizen spotted smoke near vehicle MH 12 AB 1234, call 9876543210 or email citizen@test.com."
    sanitized, redactions = PrivacyGuard.screen_text(raw_text)

    assert "MH 12 AB 1234" not in sanitized
    assert "9876543210" not in sanitized
    assert "citizen@test.com" not in sanitized
    assert "[REDACTED_VEHICLE_PLATE]" in sanitized
    assert "[REDACTED_PHONE_NUMBER]" in sanitized
    assert "[REDACTED_EMAIL]" in sanitized
    assert len(redactions) == 3


def test_grounding_validator_catches_causal_violations(mock_f3_payload):
    """Verifies that prohibited causal statements are caught by the grounding guard."""
    explanation = AeroSentinelGeminiPipeline.generate_explanation(mock_f3_payload)
    
    # Inject an illegal causal claim
    explanation.event_summary_analyst += " The nearby factory caused the pollution spike."
    
    result = GroundingValidator.validate_grounding(mock_f3_payload, explanation)
    assert result.is_grounded is False
    assert any("caused" in c.lower() for c in result.unsupported_claims)


def test_grounding_validator_catches_unsupported_forecast_numbers(mock_f3_payload):
    """Verifies that hallucinated numbers not in the F3 payload are flagged."""
    explanation = AeroSentinelGeminiPipeline.generate_explanation(mock_f3_payload)
    
    # Replace valid forecast string with a fabricated value
    explanation.event_summary_analyst = "PM2.5 is nominal."
    explanation.forecast_trajectory = "PM2.5 predicted at 999.9 ug/m3."
    
    result = GroundingValidator.validate_grounding(mock_f3_payload, explanation)
    assert result.is_grounded is False
    assert any("forecast" in c.lower() for c in result.unsupported_claims)


def test_vision_pipeline_with_synthetic_test_image(tmp_path):
    """Generates an in-memory image and verifies structured vision response."""
    test_img_path = tmp_path / "test_smoke.jpg"
    img = Image.new("RGB", (200, 200), color=(100, 100, 100))
    img.save(test_img_path)

    vision_res = analyze_citizen_image(test_img_path)
    assert isinstance(vision_res, CitizenVisionAnalysis)
    assert 0.0 <= vision_res.visual_confidence_estimate <= 1.0
    assert len(vision_res.limitations) >= 2


def test_pipeline_with_multimodal_citizen_input(mock_f3_payload, tmp_path):
    """Tests end-to-end explanation pipeline with multimodal image fusion."""
    test_img = tmp_path / "citizen_sample.jpg"
    Image.new("RGB", (100, 100), color="gray").save(test_img)

    explanation = AeroSentinelGeminiPipeline.generate_explanation(
        f3_payload=mock_f3_payload,
        citizen_image=test_img
    )

    assert explanation.visual_evidence is not None
    assert explanation.causal_claim_supported is False
    assert "886196944dfffff" in explanation.event_summary_public
    assert "142.5" in explanation.event_summary_analyst
    assert explanation.prompt_version in [
        PROMPT_VERSIONS["structured_event_explanation"],
        PROMPT_VERSIONS["event_explanation"]
    ]


def test_conflicting_evidence_handling(mock_f3_payload):
    """Tests that conflicting evidence is classified without forcing agreement."""
    # Modify payload to have contradictory signals: high ground PM2.5 but 0 fires and clean satellite
    mock_f3_payload["evidence"].append({
        "category": "REMOTE_SENSING",
        "source": "SENTINEL_5P_TROPOMI",
        "metric": "satellite_no2_trop",
        "attribution_note": "Tropospheric column shows no detectable anomaly (nominal baseline)."
    })
    
    explanation = AeroSentinelGeminiPipeline.generate_explanation(mock_f3_payload)
    assert explanation.evidence_summary.evidence_consistency in [
        "consistent", "partially_consistent", "conflicting", "insufficient_evidence"
    ]
    assert explanation.causal_claim_supported is False