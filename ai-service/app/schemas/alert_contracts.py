"""
AeroSentinel - Phase 5 Alert-Support Contracts & Event Evidence Schemas
File: ai-service/app/schemas/alert_contracts.py
Fulfills Section 7 (Alert-Support Features), Section 12 (Score Range),
Section 14 & 15 (Recency Factor), Section 19 & 20 (4-Valued Multi-Source State),
Section 23 (Alert Support State), Section 25 (Event Evidence Object),
Section 33 & 34 (Clustering & Deduplication), Section 43 (Score Breakdown),
Section 46 (Backend Contract), and Section 47 (Frontend Contract).
"""

from typing import List, Dict, Any, Optional
from enum import Enum
from pydantic import BaseModel, Field


class AlertSupportState(str, Enum):
    INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE"
    MONITOR = "MONITOR"
    ALERT_CANDIDATE = "ALERT_CANDIDATE"


class EvidenceConsistencyTier(str, Enum):
    CONSISTENT = "consistent"
    PARTIALLY_CONSISTENT = "partially_consistent"
    CONFLICTING = "conflicting"
    INSUFFICIENT_EVIDENCE = "insufficient_evidence"


class SourceObservationState(str, Enum):
    """
    Fulfills Section 20: Strict distinction between not detected and unavailable.
    Never treat unavailable data as negative confirmation.
    """
    SUPPORTED = "supported"
    NOT_DETECTED = "not_detected"
    UNAVAILABLE = "unavailable"
    UNKNOWN = "unknown"


class MultiSourceStateMatrix(BaseModel):
    """
    Explicit 4-valued observation state across all independent sensing tiers.
    Fulfills Section 8, 9, 19, and 20.
    """
    ground_sensor: SourceObservationState = SourceObservationState.UNKNOWN
    satellite: SourceObservationState = SourceObservationState.UNKNOWN
    fire: SourceObservationState = SourceObservationState.UNKNOWN
    meteorology: SourceObservationState = SourceObservationState.UNKNOWN
    gis: SourceObservationState = SourceObservationState.UNKNOWN
    citizen: SourceObservationState = SourceObservationState.UNKNOWN
    ml_model: SourceObservationState = SourceObservationState.UNKNOWN


class SourcePresenceBreakdown(BaseModel):
    """Legacy boolean presence indicators for backwards compatibility."""
    ground_sensor_present: bool = False
    satellite_present: bool = False
    fire_present: bool = False
    meteorology_present: bool = False
    gis_present: bool = False
    citizen_present: bool = False
    ml_present: bool = False


class AlertLeadTimeMetrics(BaseModel):
    """Timing and forward persistence indicators derived from F3 forecasting."""
    forecast_horizon_hours: int = 6
    predicted_persistence_hours: float = Field(..., ge=0.0)
    lead_time_to_peak_hours: int = Field(default=1)
    peak_predicted_pm25: float = Field(..., ge=0.0)
    uncertainty_range_width: float = Field(..., ge=0.0)


class EventEvidenceScoreBreakdown(BaseModel):
    """
    Transparent, auditable score decomposition. Range: [0.0, 1.0].
    Fulfills Section 14, 15, 42 (Auditability) and Section 43 (Score Breakdown).
    """
    observation_strength: float = Field(..., ge=0.0, le=1.0)
    ml_forecast_support: float = Field(..., ge=0.0, le=1.0)
    multi_source_agreement: float = Field(..., ge=0.0, le=1.0)
    spatial_consistency: float = Field(..., ge=0.0, le=1.0)
    temporal_persistence: float = Field(..., ge=0.0, le=1.0)
    recency_factor: float = Field(default=1.0, ge=0.0, le=1.0)
    conflict_penalty: float = Field(..., ge=0.0, le=1.0)
    evidence_completeness: float = Field(..., ge=0.0, le=1.0)
    final_evidence_score: float = Field(..., ge=0.0, le=1.0)


class EventGroupingMetadata(BaseModel):
    """
    Deterministic deduplication and event cluster reference.
    Fulfills Section 33 (Event Deduplication) and Section 34 (Event ID).
    """
    canonical_event_id: str
    primary_h3_cell: Optional[str] = None
    cluster_h3_cells: List[str] = Field(default_factory=list)
    time_window_start: str
    time_window_end: str
    is_primary_cell: bool = True
    deduplication_hash: str
    cluster_size: int = Field(default=1, ge=1)


class AlertSupportPayload(BaseModel):
    """
    Official F5 Output Contract consumed by:
    - Member 1 (Java/Spring Boot) for Alert Review & Workflow Transition (BACKEND CONTRACT REQUIRED)
    - Member 2 (React Frontend) for Inspector Drawer & Score Visualizations (FRONTEND CONTRACT REQUIRED)
    """
    event_id: str
    timestamp: str
    h3_cell_id: str
    alert_support_state: AlertSupportState
    evidence_score: float = Field(..., ge=0.0, le=1.0)
    evidence_completeness: float = Field(..., ge=0.0, le=1.0)
    evidence_consistency: EvidenceConsistencyTier
    
    score_breakdown: EventEvidenceScoreBreakdown
    source_matrix: MultiSourceStateMatrix = Field(default_factory=MultiSourceStateMatrix)
    sources_present: SourcePresenceBreakdown = Field(default_factory=SourcePresenceBreakdown)
    lead_time_metrics: AlertLeadTimeMetrics
    grouping: EventGroupingMetadata

    supporting_signals: List[str] = Field(default_factory=list)
    unavailable_sources: List[str] = Field(default_factory=list)
    conflicting_notes: List[str] = Field(default_factory=list)

    gemini_summary_public: Optional[str] = None
    gemini_summary_analyst: Optional[str] = None

    scoring_version: str = "v1.0.0"
    model_version: str = "v1.0.0"
    feature_version: str = "v1.0.0"