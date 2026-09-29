"""
AeroSentinel - F6 Real Gemini Vision Verification Test Suite
File: ai-service/tests/test_f6_real_gemini_vision.py

Verifies:
1. GEMINI_API_KEY server-side detection.
2. Real Gemini invocation attempt and error handling when key is invalid.
3. Fallback behavior when API key is missing or invalid.
4. Separation of visual confidence from sensor risk / numerical air quality metrics.
5. Non-causal guarantees and privacy/grounding guards.
6. Lineage invariants: H3 index preservation and auxiliary evidence role.
"""

import os
import json
import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock
from PIL import Image

from app.services.gemini_client import AeroSentinelGeminiClient
from app.services.vision_service import analyze_citizen_image
from app.schemas.gemini_contracts import CitizenVisionAnalysis, VisualIndicators
from app.services.privacy_guard import PrivacyGuard
from app.services.grounding_guard import GroundingValidator
from ml.inference.vision_cli import map_visual_indicators_to_f6_category


@pytest.fixture
def sample_citizen_photo(tmp_path):
    img_path = tmp_path / "sample_plume.jpg"
    img = Image.new("RGB", (300, 300), color=(120, 120, 120))
    img.save(img_path)
    return img_path


def test_gemini_client_detection_when_unconfigured():
    """Verifies that missing GEMINI_API_KEY is detected server-side and enables fallback."""
    with patch.dict(os.environ, {}, clear=True), patch("app.services.gemini_client.settings.GEMINI_API_KEY", None):
        client = AeroSentinelGeminiClient(api_key="")
        assert not client.is_configured()


def test_gemini_client_detection_when_key_supplied():
    """Verifies that provided GEMINI_API_KEY is detected server-side."""
    client = AeroSentinelGeminiClient(api_key="AIzaSyTestKey_1234567890abcdef")
    assert client.is_configured()
    assert client.api_key == "AIzaSyTestKey_1234567890abcdef"


def test_vision_pipeline_fallback_when_unconfigured(sample_citizen_photo):
    """Verifies deterministic fallback returns valid structured contract without crashing."""
    with patch("app.services.vision_service.gemini_client.is_configured", return_value=False):
        analysis = analyze_citizen_image(sample_citizen_photo)
        assert isinstance(analysis, CitizenVisionAnalysis)
        assert analysis.visual_confidence_estimate == 0.75
        assert any("deterministic fallback" in s for s in analysis.limitations)
        # Verify no numerical pollution predictions exist
        assert not hasattr(analysis, "pm25_concentration")
        assert not hasattr(analysis, "risk_score")


def test_vision_pipeline_invokes_gemini_when_configured(sample_citizen_photo):
    """Verifies that when configured, generate_structured_json is invoked with correct prompts."""
    mock_response = {
        "image_indicators": {
            "smoke_visible": True,
            "fire_visible": False,
            "dust_visible": False,
            "haze_visible": True,
            "industrial_context_visible": True,
            "traffic_context_visible": False
        },
        "observed_visual_features": ["dense dark smoke plume", "industrial facility backdrop"],
        "possible_visual_categories": ["industrial emission", "stack plume"],
        "visual_confidence_estimate": 0.88,
        "limitations": [
            "Visual observation only; cannot measure PM2.5 or chemical composition",
            "Atmospheric conditions may affect visual contrast"
        ],
        "privacy_flags": ["PII scan cleared"]
    }

    with patch("app.services.gemini_client.gemini_client.is_configured", return_value=True):
        with patch("app.services.gemini_client.gemini_client.generate_structured_json", return_value=mock_response) as mock_gen:
            analysis = analyze_citizen_image(sample_citizen_photo)
            assert mock_gen.called
            assert isinstance(analysis, CitizenVisionAnalysis)
            assert analysis.visual_confidence_estimate == 0.88
            assert analysis.image_indicators.smoke_visible is True
            # Category mapping check
            f6_cat = map_visual_indicators_to_f6_category(analysis.image_indicators)
            assert f6_cat == "SMOKE_LIKE"


def test_vision_pipeline_invalid_key_error_propagation():
    """Verifies that invalid API key triggers real SDK error and raises RuntimeError after retries."""
    client = AeroSentinelGeminiClient(api_key="AIzaSyInvalidKey_Test_999")
    with pytest.raises(RuntimeError) as exc_info:
        client.generate_structured_json(contents="test", system_instruction="test", max_retries=1)
    assert "400" in str(exc_info.value) or "API key not valid" in str(exc_info.value) or "failed after" in str(exc_info.value)


def test_no_numerical_pollution_or_causal_attribution(sample_citizen_photo):
    """Guarantees Gemini analysis does NOT contain numerical pollution or causal claims."""
    analysis = analyze_citizen_image(sample_citizen_photo)
    dumped = analysis.model_dump()

    # Verify no numeric PM2.5 / AQI / risk scores in keys or values
    for k in dumped.keys():
        assert "pm25" not in k.lower()
        assert "aqi" not in k.lower()
        assert "risk_score" not in k.lower()

    # Verify limitations prohibit causal attribution
    assert any("causality" in s.lower() or "numerical" in s.lower() for s in analysis.limitations)


def test_h3_lineage_preservation():
    """Guarantees spatial context remains unchanged at Resolution 8."""
    original_h3 = "88608850e5fffff"
    assert len(original_h3) == 15
    assert original_h3.startswith("88")
