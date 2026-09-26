"""
AeroSentinel - Evidence Recency Calculator
File: ai-service/ml/alert_support/recency.py
Fulfills Section 14 (Evidence Quality) & Section 15 (Evidence Recency).
"""

from datetime import datetime, timezone
from typing import Dict, Any, List


class EvidenceRecencyHandler:
    """Applies half-life exponential time-decay discounting to telemetry."""

    HALF_LIVES_MINUTES = {
        "GROUND": 120.0,      # Ground CAAQMS: 2 hours
        "FIRE": 180.0,        # NASA FIRMS VIIRS: 3 hours
        "SATELLITE": 360.0,   # Sentinel-5P: 6 hours
        "CITIZEN": 90.0,      # Citizen image/report: 1.5 hours
        "METEO": 180.0        # Weather observation: 3 hours
    }

    @classmethod
    def calculate_recency_weight(cls, source_type: str, item_time_iso: str, ref_time_iso: str) -> float:
        try:
            t_item = datetime.fromisoformat(item_time_iso.replace("Z", "+00:00"))
            t_ref = datetime.fromisoformat(ref_time_iso.replace("Z", "+00:00"))
            age_minutes = max(0.0, (t_ref - t_item).total_seconds() / 60.0)
        except Exception:
            return 1.0

        half_life = cls.HALF_LIVES_MINUTES.get(source_type.upper(), 180.0)
        # Exponential decay: e^(-ln(2) * age / half_life)
        decay = (0.5) ** (age_minutes / half_life)
        return round(max(0.10, min(1.0, decay)), 3)

    @classmethod
    def evaluate_payload_recency(cls, evidence_list: List[Dict[str, Any]], ref_time_iso: str) -> float:
        if not evidence_list:
            return 1.0
        weights = []
        for item in evidence_list:
            cat = item.get("category") or item.get("type", "GROUND")
            item_time = item.get("timestamp", ref_time_iso)
            weights.append(cls.calculate_recency_weight(cat, item_time, ref_time_iso))
        return round(sum(weights) / len(weights), 3) if weights else 1.0