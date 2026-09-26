"""
AeroSentinel - Secure Gemini Client & Resilience Wrapper
File: ai-service/app/services/gemini_client.py
Fulfills Section 26 (Deterministic Settings), Section 27 (Retry Strategy),
Section 28 (Model Configuration), and Section 29 (API Key Security).
"""

import os
import json
import logging
from typing import Optional, Dict, Any
from pathlib import Path

from google import genai
from google.genai import types

from app.utils.config import settings

logger = logging.getLogger("AeroSentinel.GeminiClient")


class AeroSentinelGeminiClient:
    """Production wrapper for Google GenAI SDK with structured response validation and retries."""

    def __init__(self, api_key: Optional[str] = None, model_name: Optional[str] = None):
        self.api_key = (
            api_key
            or os.getenv("GEMINI_API_KEY")
            or getattr(settings, "GEMINI_API_KEY", None)
        )
        self.model_name = (
            model_name
            or os.getenv("GEMINI_MODEL")
            or getattr(settings, "GEMINI_MODEL", "gemini-2.0-flash")
        )
        
        if self.api_key:
            self.client = genai.Client(api_key=self.api_key)
        else:
            self.client = None
            logger.warning("Gemini Client initialized without GEMINI_API_KEY. Synthetic fallbacks enabled.")

    def is_configured(self) -> bool:
        return self.client is not None and bool(self.api_key)

    def generate_structured_json(
        self,
        contents: Any,
        system_instruction: str,
        max_retries: int = 2
    ) -> Dict[str, Any]:
        """
        Calls Gemini with strict JSON mode, low temperature, and controlled retry logic.
        """
        if not self.is_configured():
            raise RuntimeError(
                "Gemini API is not configured. Please supply a valid GEMINI_API_KEY environment variable."
            )

        config = types.GenerateContentConfig(
            system_instruction=system_instruction,
            temperature=0.1,  # Low variance for factual, non-hallucinatory output
            response_mime_type="application/json",
            max_output_tokens=2048,
        )

        attempts = 0
        last_error = None

        while attempts <= max_retries:
            try:
                attempts += 1
                response = self.client.models.generate_content(
                    model=self.model_name,
                    contents=contents,
                    config=config
                )
                
                raw_text = response.text.strip()
                # Parse JSON safely
                parsed = json.loads(raw_text)
                return parsed

            except (json.JSONDecodeError, Exception) as e:
                last_error = e
                logger.warning(f"Gemini generation attempt {attempts} failed: {e}")
                if attempts > max_retries:
                    break

        raise RuntimeError(f"Gemini structured generation failed after {attempts} attempts. Error: {last_error}")


# Global singleton instance
gemini_client = AeroSentinelGeminiClient()