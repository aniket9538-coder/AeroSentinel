"""
AeroSentinel - Privacy Screening & PII Redaction Guard (F4)
File: ai-service/app/services/privacy_guard.py
Fulfills Section 9 (Image Safety / Privacy).
Screens citizen text and imagery metadata for personal identifiable information (PII).
"""

import re
from typing import Dict, Any, List, Tuple


class PrivacyGuard:
    """Detects and redacts personal identifiable information from citizen contributions."""

    # Regex patterns for Indian phone numbers, vehicle registrations, and email addresses
    PHONE_PATTERN = re.compile(r"(?:\+91[\-\s]?)?[6789]\d{9}")
    VEHICLE_PLATE_PATTERN = re.compile(r"\b[A-Z]{2}[-\s]?[0-9]{1,2}[-\s]?[A-Z]{1,3}[-\s]?[0-9]{4}\b", re.IGNORECASE)
    EMAIL_PATTERN = re.compile(r"[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+")

    @classmethod
    def screen_text(cls, text: str) -> Tuple[str, List[str]]:
        """Screens citizen text input and redacts detected PII."""
        redactions = []
        sanitized = text

        # Check vehicle plates
        for match in cls.VEHICLE_PLATE_PATTERN.finditer(text):
            plate = match.group()
            sanitized = sanitized.replace(plate, "[REDACTED_VEHICLE_PLATE]")
            redactions.append(f"Redacted vehicle registration plate: {plate[:4]}****")

        # Check phone numbers
        for match in cls.PHONE_PATTERN.finditer(text):
            phone = match.group()
            sanitized = sanitized.replace(phone, "[REDACTED_PHONE_NUMBER]")
            redactions.append("Redacted citizen telephone number")

        # Check emails
        for match in cls.EMAIL_PATTERN.finditer(text):
            email = match.group()
            sanitized = sanitized.replace(email, "[REDACTED_EMAIL]")
            redactions.append("Redacted citizen email address")

        return sanitized, redactions

    @classmethod
    def sanitize_vision_analysis(cls, vision_data: Dict[str, Any]) -> Dict[str, Any]:
        """Ensures no personal identifying markers leak into visual features or categories."""
        sanitized = dict(vision_data)
        features = sanitized.get("observed_visual_features", [])
        clean_features = []
        privacy_flags = list(sanitized.get("privacy_flags", []))

        for feat in features:
            cleaned, flags = cls.screen_text(feat)
            clean_features.append(cleaned)
            privacy_flags.extend(flags)

        sanitized["observed_visual_features"] = clean_features
        sanitized["privacy_flags"] = list(set(privacy_flags))
        return sanitized