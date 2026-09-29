"""
AeroSentinel - F5-P7 Failure Recovery & Operational Hardening Tests
File: ai-service/tests/test_f5_p7_failure_recovery.py
Validates failure resilience, degraded modes, deterministic fallbacks,
and partial evidence handling per F5-P7 specification.
"""

import pytest
from unittest.mock import patch, MagicMock
from app.services.gemini_pipeline import AeroSentinelGeminiPipeline
from app.schemas.gemini_contracts import StructuredEventExplanation
from app.schemas.alert_contracts import (
    SourceObservationState,
    EvidenceConsistencyTier,
)
from app.utils.alert_config import alert_config
from ml.alert_support.scoring_engine import EventEvidenceScoringEngine


@pytest.fixture
def sample_f3_payload():
    return {
        "timestamp": "2026-09-28T12:00:00Z",
        "h3_cell_id": "88608850e5fffff",
        "model_version": "hotspot_classifier_v1",
        "hotspot": {
            "calibrated_hotspot_probability": 0.78,
            "operational_threshold": 0.20,
            "is_hotspot": True,
            "governance_claim": "Associated statistical observations only."
        },
        "forecast": {
            "pm25_t_plus_1h": 72.0,
            "pm25_t_plus_3h": 70.0,
            "pm25_t_plus_6h": 65.0,
            "uncertainty_lower_bound_p10": 60.0,
            "uncertainty_upper_bound_p90": 80.0
        },
        "confidence": {
            "overall_confidence": 0.86,
            "model_margin_confidence": 0.80,
            "data_completeness_score": 0.90,
            "spatial_coverage_confidence": 0.95
        },
        "evidence": [
            {
                "category": "DIRECT_OBSERVATION",
                "source": "CPCB_CAAQMS_PUN001",
                "metric": "pm25_concentration",
                "value": 78.0,
                "attribution_note": "Ground PM2.5 measurement (78.0 ug/m3) at reference monitor."
            },
            {
                "category": "GIS_CONTEXT",
                "source": "MIDC_SURVEY",
                "metric": "dist_to_nearest_industrial_km",
                "value": 3.5,
                "attribution_note": "Proximity to industrial cluster: 3.5 km."
            }
        ]
    }


class TestF5P7FailureRecovery:

    def test_p7_1_gemini_unconfigured_uses_deterministic_fallback(self, sample_f3_payload):
        """When Gemini is not configured, pipeline produces deterministic grounded fallback."""
        with patch("app.services.gemini_pipeline.gemini_client.is_configured", return_value=False):
            explanation = AeroSentinelGeminiPipeline.generate_explanation(sample_f3_payload)

            assert isinstance(explanation, StructuredEventExplanation)
            assert explanation.model_version == "deterministic-fallback-v1.0"
            assert "88608850e5fffff" in explanation.event_summary_public
            assert explanation.causal_claim_supported is False
            assert len(explanation.unsupported_conclusions) >= 1

    def test_p7_2_gemini_api_exception_falls_back_gracefully(self, sample_f3_payload):
        """When Gemini API throws an exception (timeout, quota, network), fallback activates."""
        with patch("app.services.gemini_pipeline.gemini_client.is_configured", return_value=True), \
             patch("app.services.gemini_pipeline.gemini_client.generate_structured_json", side_effect=RuntimeError("API Gateway Timeout (504)")):

            explanation = AeroSentinelGeminiPipeline.generate_explanation(sample_f3_payload)

            assert isinstance(explanation, StructuredEventExplanation)
            assert explanation.model_version == "deterministic-fallback-v1.0"
            assert explanation.causal_claim_supported is False

    def test_p7_3_gemini_malformed_json_falls_back_gracefully(self, sample_f3_payload):
        """When Gemini returns schema-incompatible JSON, validation failure falls back gracefully."""
        corrupted_response = {"corrupted_key": 12345}  # Missing required fields

        with patch("app.services.gemini_pipeline.gemini_client.is_configured", return_value=True), \
             patch("app.services.gemini_pipeline.gemini_client.generate_structured_json", return_value=corrupted_response):

            explanation = AeroSentinelGeminiPipeline.generate_explanation(sample_f3_payload)

            assert isinstance(explanation, StructuredEventExplanation)
            assert explanation.model_version == "deterministic-fallback-v1.0"
            assert explanation.causal_claim_supported is False

    def test_p7_4_grounding_guard_blocks_unsupported_causal_claims(self, sample_f3_payload):
        """Grounding guard enforces non-causality flag regardless of model generation."""
        mock_output = {
            "event_summary_public": "Test summary",
            "event_summary_analyst": "Test analyst summary",
            "detected_condition": "Smoke plume from local kiln",
            "supporting_signals": ["PM2.5 elevated"],
            "forecast_trajectory": "Elevated PM2.5 persists",
            "uncertainty_and_confidence_statement": "High certainty",
            "unsupported_conclusions": [],
            "causal_claim_supported": True,  # Unsafe model hallucination!
            "evidence_summary": {
                "evidence_consistency": "consistent",
                "categorized_evidence": {
                    "direct_observations": ["PM2.5: 78.0 ug/m3"],
                    "remote_sensing_signals": [],
                    "meteorological_context": [],
                    "gis_context": [],
                    "model_signals": ["Risk score: 0.78"]
                },
                "missing_evidence_notes": [],
                "conflicting_evidence_notes": []
            },
            "prompt_version": "v1.0",
            "model_version": "gemini-2.0-flash"
        }

        with patch("app.services.gemini_pipeline.gemini_client.is_configured", return_value=True), \
             patch("app.services.gemini_pipeline.gemini_client.generate_structured_json", return_value=mock_output):

            explanation = AeroSentinelGeminiPipeline.generate_explanation(sample_f3_payload)

            # GroundingValidator must sanitize causal_claim_supported to False
            assert explanation.causal_claim_supported is False

    def test_p7_5_partial_evidence_four_valued_states_preserved(self, sample_f3_payload):
        """F5 source matrix must preserve supported, not_detected, unavailable, unknown distinctly."""
        engine = EventEvidenceScoringEngine(config=alert_config)
        explanation = AeroSentinelGeminiPipeline.generate_explanation(sample_f3_payload)
        explanation_dict = explanation.model_dump()

        result = engine.generate_alert_support_payload(
            f3_payload=sample_f3_payload,
            f4_explanation=explanation_dict,
            neighbor_payloads=[],
            raw_citizen_reports=[]
        )

        matrix = result.source_matrix
        # Ground sensor is supported
        assert matrix.ground_sensor == SourceObservationState.SUPPORTED
        # Satellite was not in evidence list -> UNAVAILABLE (not not_detected!)
        assert matrix.satellite == SourceObservationState.UNAVAILABLE
        assert SourceObservationState.UNAVAILABLE.value != SourceObservationState.NOT_DETECTED.value

    def test_p7_6_stale_data_recency_decay_reduces_score(self, sample_f3_payload):
        """Stale telemetry must receive recency penalty and never be claimed as fresh."""
        engine = EventEvidenceScoringEngine(config=alert_config)
        explanation = AeroSentinelGeminiPipeline.generate_explanation(sample_f3_payload)
        explanation_dict = explanation.model_dump()

        # Fresh payload
        fresh_payload = dict(sample_f3_payload)
        fresh_payload["timestamp"] = "2026-09-28T14:00:00Z"
        fresh_payload["evidence"] = [
            dict(item, timestamp="2026-09-28T14:00:00Z") for item in sample_f3_payload["evidence"]
        ]
        res_fresh = engine.generate_alert_support_payload(fresh_payload, explanation_dict)

        # Stale payload (evidence 8 hours old)
        stale_payload = dict(sample_f3_payload)
        stale_payload["timestamp"] = "2026-09-28T14:00:00Z"
        stale_payload["evidence"] = [
            dict(item, timestamp="2026-09-28T06:00:00Z") for item in sample_f3_payload["evidence"]
        ]
        res_stale = engine.generate_alert_support_payload(stale_payload, explanation_dict)

        # Recency factor must decay and score must be strictly lower
        assert res_stale.score_breakdown.recency_factor < 1.0
        assert res_stale.evidence_score < res_fresh.evidence_score
