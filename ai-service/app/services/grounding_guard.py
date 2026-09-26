"""
AeroSentinel - Hallucination Guard & Grounding Validator (F4)
File: ai-service/app/services/grounding_guard.py
Fulfills Section 5 (No Invented Facts), Section 24 (Hallucination Guard),
and Section 36 (Evidence Grounding Test).
"""

import re
from typing import Dict, Any, List
from app.schemas.gemini_contracts import StructuredEventExplanation, GroundingCheckResult


class GroundingValidator:
    """
    Rigorous verification engine ensuring Gemini explanations are strictly anchored
    in the deterministic F3 output and adhere to environmental governance rules.
    """

    PROHIBITED_CAUSAL_PHRASES = [
        "caused the pollution",
        "caused this event",
        "factory caused",
        "plant caused",
        "responsible for causing",
        "proves the facility is at fault",
        "proves violation",
        "guilty of violation"
    ]

    @classmethod
    def validate_grounding(
        cls,
        f3_payload: Dict[str, Any],
        explanation: StructuredEventExplanation
    ) -> GroundingCheckResult:
        unsupported_claims = []
        grounded_count = 0
        missing_signals = []

        full_text = (
            f"{explanation.event_summary_public} "
            f"{explanation.event_summary_analyst} "
            f"{' '.join(explanation.supporting_signals)} "
            f"{explanation.forecast_trajectory}"
        ).lower()

        # Check 1: Enforce Strict Non-Causality
        if explanation.causal_claim_supported:
            unsupported_claims.append("Violation: causal_claim_supported flag set to True.")

        for phrase in cls.PROHIBITED_CAUSAL_PHRASES:
            if phrase in full_text:
                unsupported_claims.append(f"Causality Violation: Detected prohibited claim '{phrase}'")

        # Check 2: Verify Grounding of Numerical Forecasts
        forecast_data = f3_payload.get("forecast", {})
        for key in ["pm25_t_plus_1h", "pm25_t_plus_3h", "pm25_t_plus_6h"]:
            if key in forecast_data:
                val = round(float(forecast_data[key]), 1)
                # Check if the number appears in the analyst text
                val_pattern = rf"\b{int(val)}\b|\b{val:.1f}\b"
                if re.search(val_pattern, explanation.event_summary_analyst) or re.search(val_pattern, explanation.forecast_trajectory):
                    grounded_count += 1
                else:
                    unsupported_claims.append(f"Missing Grounding: Expected forecast value {val} ug/m3 for {key}")

        # Check 3: Verify Grounding of Hotspot Decision & Probability
        hotspot_data = f3_payload.get("hotspot", {})
        prob = hotspot_data.get("calibrated_hotspot_probability")
        if prob is not None:
            prob_pct = int(round(prob * 100))
            prob_pattern = rf"\b{prob:.2f}\b|\b{prob_pct}%\b"
            if re.search(prob_pattern, full_text):
                grounded_count += 1

        # Check 4: Verify Input Evidence Signals Representation
        evidence_signals = f3_payload.get("evidence", [])
        for sig in evidence_signals:
            sig_metric = sig.get("metric", "").lower()
            sig_source = sig.get("source", "").lower()
            if sig_metric in full_text or sig_source in full_text:
                grounded_count += 1

        is_grounded = len(unsupported_claims) == 0

        return GroundingCheckResult(
            is_grounded=is_grounded,
            unsupported_claims=unsupported_claims,
            grounded_entities_count=grounded_count,
            missing_required_signals=missing_signals,
            sanitized_explanation=explanation if is_grounded else None
        )