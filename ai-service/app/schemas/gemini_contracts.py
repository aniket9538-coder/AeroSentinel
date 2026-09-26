"""
AeroSentinel - Phase 4 Gemini Schemas & Output Contracts
File: ai-service/app/schemas/gemini_contracts.py
Fulfills Section 8 (Vision Schema), Section 12 (Explanation Schema), 
Section 16 (Evidence Summary), and Section 23 (Output Validation).
"""

from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


# -------------------------------------------------------------
# 1. Vision Schemas (Section 8)
# -------------------------------------------------------------

class VisualIndicators(BaseModel):
    smoke_visible: bool = Field(default=False)
    fire_visible: bool = Field(default=False)
    dust_visible: bool = Field(default=False)
    haze_visible: bool = Field(default=False)
    industrial_context_visible: bool = Field(default=False)
    traffic_context_visible: bool = Field(default=False)


class CitizenVisionAnalysis(BaseModel):
    """Structured result produced by Gemini Vision on citizen-submitted imagery."""
    image_indicators: VisualIndicators
    observed_visual_features: List[str] = Field(default_factory=list)
    possible_visual_categories: List[str] = Field(default_factory=list)
    visual_confidence_estimate: float = Field(..., ge=0.0, le=1.0)
    limitations: List[str] = Field(default_factory=list)
    privacy_flags: List[str] = Field(
        default_factory=list,
        description="Flags if any face, license plate, or personal data was detected and omitted."
    )


# -------------------------------------------------------------
# 2. Evidence Summary Schemas (Section 16, 18)
# -------------------------------------------------------------

class CategorizedEvidence(BaseModel):
    direct_observations: List[str] = Field(default_factory=list)
    remote_sensing_signals: List[str] = Field(default_factory=list)
    meteorological_context: List[str] = Field(default_factory=list)
    gis_context: List[str] = Field(default_factory=list)
    model_signals: List[str] = Field(default_factory=list)


class EvidenceSummaryOutput(BaseModel):
    evidence_consistency: str = Field(
        ...,
        description="consistent | partially_consistent | conflicting | insufficient_evidence"
    )
    categorized_evidence: CategorizedEvidence
    missing_evidence_notes: List[str] = Field(default_factory=list)
    conflicting_evidence_notes: List[str] = Field(default_factory=list)


# -------------------------------------------------------------
# 3. Structured Event Explanation Schemas (Section 12, 13, 33)
# -------------------------------------------------------------

class StructuredEventExplanation(BaseModel):
    """
    Core structured output from Gemini interpreting F3 predictions and evidence.
    Consumed directly by Member 1 (Spring Boot / DB) and Member 2 (React).
    """
    event_summary_public: str = Field(
        ...,
        description="Plain-language, non-alarmist explanation suitable for public dashboards."
    )
    event_summary_analyst: str = Field(
        ...,
        description="Detailed analytical brief including model metrics, bounds, and indicators."
    )
    detected_condition: str
    supporting_signals: List[str] = Field(default_factory=list)
    forecast_trajectory: str
    uncertainty_and_confidence_statement: str
    unsupported_conclusions: List[str] = Field(
        default_factory=list,
        description="Explicit statements of what cannot be concluded (e.g., causality, facility blame)."
    )
    causal_claim_supported: bool = Field(
        default=False,
        description="Strictly False under non-causal attribution governance."
    )
    evidence_summary: EvidenceSummaryOutput
    visual_evidence: Optional[CitizenVisionAnalysis] = None
    prompt_version: str
    model_version: str = "gemini-2.0-flash"


# -------------------------------------------------------------
# 4. Grounding & Hallucination Guard Result (Section 24, 36)
# -------------------------------------------------------------

class GroundingCheckResult(BaseModel):
    is_grounded: bool
    unsupported_claims: List[str] = Field(default_factory=list)
    grounded_entities_count: int
    missing_required_signals: List[str] = Field(default_factory=list)
    sanitized_explanation: Optional[StructuredEventExplanation] = None