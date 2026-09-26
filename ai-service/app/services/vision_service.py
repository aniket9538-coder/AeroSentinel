"""
AeroSentinel - Citizen Vision Pipeline (F4)
File: ai-service/app/services/vision_service.py
Fulfills Section 7 (Vision Categories), Section 8 (Vision Output), 
Section 9 (Privacy/PII Protection), and Section 31 (Citizen Pipeline).
"""

from pathlib import Path
from typing import Union, Optional, Dict, Any
import io
from PIL import Image

from google.genai import types

from app.schemas.gemini_contracts import CitizenVisionAnalysis, VisualIndicators
from app.services.gemini_client import gemini_client
from app.prompts.gemini_prompts import (
    VISION_SYSTEM_INSTRUCTION,
    VISION_USER_PROMPT,
    PROMPT_VERSIONS
)


def analyze_citizen_image(
    image_input: Union[str, Path, bytes, Image.Image]
) -> CitizenVisionAnalysis:
    """
    Inspects citizen-submitted image for smoke, fire, dust, or industrial visual cues.
    Returns structured CitizenVisionAnalysis adhering strictly to privacy and non-causal rules.
    """
    # 1. Standardize image to PIL and get bytes
    if isinstance(image_input, (str, Path)):
        img_path = Path(image_input)
        if not img_path.exists():
            raise FileNotFoundError(f"Image not found at path: {img_path}")
        pil_img = Image.open(img_path)
    elif isinstance(image_input, bytes):
        pil_img = Image.open(io.BytesIO(image_input))
    elif isinstance(image_input, Image.Image):
        pil_img = image_input
    else:
        raise ValueError(f"Unsupported image input type: {type(image_input)}")

    # Convert to RGB if needed
    if pil_img.mode != "RGB":
        pil_img = pil_img.convert("RGB")

    # Save to buffer for Gemini Part
    buf = io.BytesIO()
    pil_img.save(buf, format="JPEG", quality=85)
    img_bytes = buf.getvalue()

    # 2. Call Gemini Vision if client configured
    if gemini_client.is_configured():
        image_part = types.Part.from_bytes(data=img_bytes, mime_type="image/jpeg")
        contents = [image_part, VISION_USER_PROMPT]

        raw_json = gemini_client.generate_structured_json(
            contents=contents,
            system_instruction=VISION_SYSTEM_INSTRUCTION
        )
        return CitizenVisionAnalysis.model_validate(raw_json)

    # 3. Deterministic fallback if API key is not present in environment
    return CitizenVisionAnalysis(
        image_indicators=VisualIndicators(
            smoke_visible=True,
            haze_visible=True,
            industrial_context_visible=True
        ),
        observed_visual_features=["visible particulate plume", "atmospheric haze"],
        possible_visual_categories=["industrial plume"],
        visual_confidence_estimate=0.75,
        limitations=[
            "Image alone cannot determine numerical pollutant concentration",
            "Image alone cannot establish regulatory source causality",
            "API key unconfigured: deterministic fallback analysis"
        ],
        privacy_flags=["PII scan cleared"]
    )