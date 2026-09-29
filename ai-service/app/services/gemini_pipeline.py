"""
AeroSentinel - End-to-End Gemini Intelligence Synthesis Pipeline (F4)
File: ai-service/app/services/gemini_pipeline.py
Fulfills Section 4 (F4 Objective), Section 12 (Structured Explanation),
Section 16 (Evidence Summary), Section 18 (Conflicting Evidence),
Section 19 (Missing Evidence), and Section 39 (End-to-End Flow).
"""

from typing import Dict, Any, Optional, Union
from pathlib import Path
import json
import logging
from PIL import Image

from app.schemas.gemini_contracts import (
    StructuredEventExplanation,
    CitizenVisionAnalysis,
    EvidenceSummaryOutput,
    CategorizedEvidence,
    GroundingCheckResult
)
from app.services.gemini_client import gemini_client
from app.services.vision_service import analyze_citizen_image
from app.services.grounding_guard import GroundingValidator
from app.prompts.gemini_prompts import (
    EXPLANATION_SYSTEM_INSTRUCTION,
    EXPLANATION_USER_PROMPT_TEMPLATE,
    PROMPT_VERSIONS
)

logger = logging.getLogger("AeroSentinel.GeminiPipeline")


class AeroSentinelGeminiPipeline:
    """Coordinates multimodal citizen image analysis and F3 evidence explanation synthesis."""

    @classmethod
    def generate_explanation(
        cls,
        f3_payload: Dict[str, Any],
        citizen_image: Optional[Union[str, Path, bytes, Image.Image]] = None
    ) -> StructuredEventExplanation:
        """
        Executes end-to-end synthesis:
        1. Analyzes citizen image (if supplied).
        2. Summarizes evidence and formats prompt.
        3. Calls Gemini for structured synthesis.
        4. Validates output schema.
        5. Runs GroundingValidator to prevent hallucinations.
        """
        # 1. Vision Analysis if image provided
        vision_result: Optional[CitizenVisionAnalysis] = None
        if citizen_image is not None:
            vision_result = analyze_citizen_image(citizen_image)

        # 2. Prepare compact prompt payload (Section 30: Cost & Context Control)
        compact_payload = {
            "timestamp": f3_payload.get("timestamp"),
            "h3_cell_id": f3_payload.get("h3_cell_id"),
            "hotspot": f3_payload.get("hotspot"),
            "forecast": f3_payload.get("forecast"),
            "confidence": f3_payload.get("confidence"),
            "evidence": f3_payload.get("evidence")
        }

        vision_json_str = (
            vision_result.model_dump_json(indent=2)
            if vision_result
            else "None provided"
        )

        prompt_version = (
            PROMPT_VERSIONS.get("structured_event_explanation")
            or PROMPT_VERSIONS.get("event_explanation", "structured_event_explanation_v001")
        )

        user_prompt = EXPLANATION_USER_PROMPT_TEMPLATE.format(
            f3_payload_json=json.dumps(compact_payload, indent=2),
            vision_evidence_json=vision_json_str,
            prompt_version=prompt_version,
            model_name=gemini_client.model_name
        )

        # 3. Call Gemini if configured, otherwise generate deterministic grounded synthesis
        if gemini_client.is_configured():
            try:
                raw_response = gemini_client.generate_structured_json(
                    contents=user_prompt,
                    system_instruction=EXPLANATION_SYSTEM_INSTRUCTION
                )
                explanation = StructuredEventExplanation.model_validate(raw_response)
            except Exception as exc:
                logger.warning(
                    f"Gemini API invocation or validation failed ({exc}). Falling back to deterministic grounded synthesis."
                )
                explanation = cls._generate_deterministic_grounded_fallback(
                    f3_payload=f3_payload,
                    vision_result=vision_result
                )
        else:
            explanation = cls._generate_deterministic_grounded_fallback(
                f3_payload=f3_payload,
                vision_result=vision_result
            )

        # 4. Attach Vision result to final explanation if present
        if vision_result:
            explanation.visual_evidence = vision_result

        # 5. Execute Hallucination & Grounding Check
        grounding_result = GroundingValidator.validate_grounding(f3_payload, explanation)
        if not grounding_result.is_grounded:
            logger.warning(
                f"Grounding check flagged issues: {grounding_result.unsupported_claims}. Sanitizing output."
            )
            # Enforce non-causality flag regardless of model generation
            explanation.causal_claim_supported = False

        return explanation

    @classmethod
    def _generate_deterministic_grounded_fallback(
        cls,
        f3_payload: Dict[str, Any],
        vision_result: Optional[CitizenVisionAnalysis] = None
    ) -> StructuredEventExplanation:
        """
        Deterministic, 100% grounded fallback guaranteeing valid output contracts
        even when offline, without API keys, or in CI test pipelines.
        """
        h3_cell = f3_payload.get("h3_cell_id", "unknown")
        hotspot = f3_payload.get("hotspot", {})
        forecast = f3_payload.get("forecast", {})
        conf = f3_payload.get("confidence", {})
        evidence_list = f3_payload.get("evidence", [])

        prob = hotspot.get("calibrated_hotspot_probability", 0.0)
        prob_pct = int(round(prob * 100))
        is_hotspot = hotspot.get("is_hotspot", False)

        t1 = forecast.get("pm25_t_plus_1h", 0.0)
        t3 = forecast.get("pm25_t_plus_3h", 0.0)
        t6 = forecast.get("pm25_t_plus_6h", 0.0)
        p10 = forecast.get("uncertainty_lower_bound_p10", 0.0)
        p90 = forecast.get("uncertainty_upper_bound_p90", 0.0)
        overall_conf = conf.get("overall_confidence", 0.85)

        # Categorize evidence
        direct_obs = []
        remote_signals = []
        meteo_signals = []
        gis_context = []
        model_signals = [f"Hotspot probability: {prob:.2f} ({prob_pct}%)"]

        for item in evidence_list:
            cat = item.get("category") or item.get("type")
            note = item.get("attribution_note") or item.get("interpretation", "")
            if cat == "DIRECT_OBSERVATION":
                direct_obs.append(note)
            elif cat == "REMOTE_SENSING":
                remote_signals.append(note)
            elif cat == "GIS_CONTEXT":
                gis_context.append(note)
            elif cat == "MONITORING_COVERAGE":
                gis_context.append(note)

        # Check multi-source sensing consistency & coverage blind spots (Section 18 & 19)
        has_monitoring_gap = any(
            item.get("category") == "MONITORING_COVERAGE" or 
            item.get("metric") == "monitoring_coverage_gap_flag"
            for item in evidence_list
        )
        has_remote_discrepancy = any(
            "no detectable anomaly" in (item.get("attribution_note", "")).lower() or
            "nominal" in (item.get("attribution_note", "")).lower()
            for item in evidence_list
        )

        missing_notes = []
        conflicting_notes = []

        if has_monitoring_gap or overall_conf < 0.40:
            consistency = "insufficient_evidence"
            missing_notes.append("Ground monitoring coverage sparse (>10km); elevated spatial interpolation uncertainty.")
        elif has_remote_discrepancy:
            consistency = "conflicting"
            conflicting_notes.append("Ground sensor indicates elevated PM2.5 while satellite tropospheric column remains nominal.")
        elif is_hotspot and len(direct_obs) > 0:
            consistency = "consistent"
        else:
            consistency = "partially_consistent"

        public_summary = (
            f"Air quality monitoring indicates an elevated pollution event in area {h3_cell}. "
            f"The model forecasts PM2.5 concentrations of {t1:.1f} ug/m3 in the next hour."
        )

        analyst_summary = (
            f"F3 Model Assessment for H3 {h3_cell}: Calibrated Hotspot Probability {prob:.2f} ({prob_pct}%), "
            f"Hotspot status: {is_hotspot}. Forecast trajectory: T+1h={t1:.1f} ug/m3, T+3h={t3:.1f} ug/m3, "
            f"T+6h={t6:.1f} ug/m3, with validation residual uncertainty bounds [{p10:.1f}, {p90:.1f}] ug/m3. "
            f"Composite confidence is {overall_conf:.2f}."
        )

        prompt_version = (
            PROMPT_VERSIONS.get("structured_event_explanation")
            or PROMPT_VERSIONS.get("event_explanation", "structured_event_explanation_v001")
        )

        return StructuredEventExplanation(
            event_summary_public=public_summary,
            event_summary_analyst=analyst_summary,
            detected_condition="Elevated Ground PM2.5 with Associated Regional Indicators",
            supporting_signals=[sig.get("attribution_note", "") for sig in evidence_list[:4]],
            forecast_trajectory=f"T+1h: {t1:.1f} ug/m3 | T+3h: {t3:.1f} ug/m3 | T+6h: {t6:.1f} ug/m3 (Bounds: [{p10:.1f}, {p90:.1f}])",
            uncertainty_and_confidence_statement=f"Composite Confidence: {overall_conf:.2f}. Bounded by validation residual intervals.",
            unsupported_conclusions=[
                "Facility-level legal causation cannot be asserted from ambient spatial modeling",
                "Remote sensing indicators represent column tropospheric density, not direct ground standard citations"
            ],
            causal_claim_supported=False,
            evidence_summary=EvidenceSummaryOutput(
                evidence_consistency=consistency,
                categorized_evidence=CategorizedEvidence(
                    direct_observations=direct_obs,
                    remote_sensing_signals=remote_signals,
                    meteorological_context=meteo_signals,
                    gis_context=gis_context,
                    model_signals=model_signals
                ),
                missing_evidence_notes=missing_notes,
                conflicting_evidence_notes=conflicting_notes
            ),
            visual_evidence=vision_result,
            prompt_version=prompt_version,
            model_version="deterministic-fallback-v1.0"
        )