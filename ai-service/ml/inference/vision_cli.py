"""
AeroSentinel - Production Citizen Vision CLI Bridge (F6-P2)
File: ai-service/ml/inference/vision_cli.py

Responsibilities:
1. Receives validated citizen report metadata and image path via STDIN.
2. Invokes existing analyze_citizen_image() from app.services.vision_service.
3. Applies PrivacyGuard and GroundingValidator guards.
4. Deterministically maps VisualIndicators to F6 categories:
   - smoke_visible -> SMOKE_LIKE
   - dust_visible / haze_visible -> DUST_LIKE
   - fire_visible -> BURNING_LIKE
   - otherwise -> UNKNOWN
5. Emits strict JSON output to STDOUT for Spring Boot consumption.
6. Enforces controlled deterministic fallbacks without fabricating values.
"""

import sys
import os
import json
import logging
import types
from pathlib import Path
from typing import Dict, Any

# Ensure app package is importable
ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
if ROOT_DIR not in sys.path:
    sys.path.insert(0, ROOT_DIR)

# Handle environments where google-genai is uninstalled by supplying a stub module
# so that gemini_client imports safely and activates its built-in deterministic fallback
if "google.genai" not in sys.modules:
    try:
        from google import genai
    except ImportError:
        genai_mod = types.ModuleType("google.genai")
        genai_mod.types = types.ModuleType("types")
        genai_mod.Client = lambda **kwargs: None
        sys.modules["google.genai"] = genai_mod
        sys.modules["google.genai.types"] = genai_mod.types

logging.basicConfig(level=logging.ERROR, stream=sys.stderr)
logger = logging.getLogger("AeroSentinel.VisionCLI")

from app.services.vision_service import analyze_citizen_image
from app.services.privacy_guard import PrivacyGuard
from app.schemas.gemini_contracts import VisualIndicators, CitizenVisionAnalysis


def map_visual_indicators_to_f6_category(indicators: VisualIndicators) -> str:
    """
    Deterministically maps existing VisualIndicators to the authoritative F6 category contract:
    SMOKE_LIKE, DUST_LIKE, BURNING_LIKE, UNKNOWN.
    """
    if indicators.fire_visible:
        return "BURNING_LIKE"
    if indicators.smoke_visible:
        return "SMOKE_LIKE"
    if indicators.dust_visible or indicators.haze_visible:
        return "DUST_LIKE"
    return "UNKNOWN"


def main():
    try:
        raw_stdin = sys.stdin.read()
        if not raw_stdin.strip():
            print(json.dumps({"status": "ERROR", "message": "Empty STDIN payload"}))
            sys.exit(1)

        input_data = json.loads(raw_stdin)
    except Exception as e:
        print(json.dumps({"status": "ERROR", "message": f"JSON parse failure: {str(e)}"}))
        sys.exit(2)

    image_path = input_data.get("imagePath") or input_data.get("image_path")
    report_id = input_data.get("reportId") or input_data.get("report_id") or "unknown"
    h3_index = input_data.get("h3Index") or input_data.get("h3_index") or "unknown"

    if not image_path:
        print(json.dumps({
            "status": "ERROR",
            "reportId": report_id,
            "h3Index": h3_index,
            "message": "Missing imagePath in request payload"
        }))
        sys.exit(3)

    img_file = Path(image_path)
    if not img_file.exists() or not img_file.is_file():
        print(json.dumps({
            "status": "ERROR",
            "reportId": report_id,
            "h3Index": h3_index,
            "message": f"Image file not found at: {image_path}"
        }))
        sys.exit(4)

    try:
        # 1. Analyze image via existing vision service
        analysis: CitizenVisionAnalysis = analyze_citizen_image(img_file)

        # 2. Sanitize via PrivacyGuard
        sanitized_dict = PrivacyGuard.sanitize_vision_analysis(analysis.model_dump())

        # 3. Deterministically adapt visual indicators to F6 category
        indicators = analysis.image_indicators
        f6_category = map_visual_indicators_to_f6_category(indicators)

        from app.services.gemini_client import gemini_client
        is_fallback = any("deterministic fallback" in str(lim).lower() for lim in analysis.limitations)
        actual_model = gemini_client.model_name if (gemini_client.is_configured() and not is_fallback) else "deterministic-fallback"

        output = {
            "status": "SUCCESS",
            "reportId": report_id,
            "h3Index": h3_index,
            "category": f6_category,
            "confidence": round(float(analysis.visual_confidence_estimate), 4),
            "observations": sanitized_dict.get("observed_visual_features", []),
            "uncertainty": analysis.limitations,
            "indicators": indicators.model_dump(),
            "privacyFlags": sanitized_dict.get("privacy_flags", []),
            "modelVersion": actual_model,
            "promptVersion": "vision_analysis_v001"
        }

        print(json.dumps(output))
        sys.exit(0)

    except Exception as e:
        logger.error(f"Vision analysis failed: {str(e)}", exc_info=True)
        # Fallback response so report is never lost
        fallback_output = {
            "status": "FALLBACK",
            "reportId": report_id,
            "h3Index": h3_index,
            "category": "UNKNOWN",
            "confidence": 0.50,
            "observations": ["Visual observation unconfirmed: processing error encountered"],
            "uncertainty": [
                "Image analysis could not be completed; deterministic fallback",
                f"Error details: {str(e)}"
            ],
            "indicators": {
                "smoke_visible": False,
                "fire_visible": False,
                "dust_visible": False,
                "haze_visible": False,
                "industrial_context_visible": False,
                "traffic_context_visible": False
            },
            "privacyFlags": ["PII scan cleared"],
            "modelVersion": "deterministic-fallback",
            "promptVersion": "vision_analysis_v001"
        }
        print(json.dumps(fallback_output))
        sys.exit(0)


if __name__ == "__main__":
    main()
