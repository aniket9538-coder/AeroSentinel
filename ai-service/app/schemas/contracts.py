"""
AeroSentinel - Production AI Output Contract Schemas (F3)
File: ai-service/app/schemas/contracts.py
BACKEND CONTRACT REQUIRED (Member 1) & FRONTEND CONTRACT REQUIRED (Member 2)
"""

from typing import List, Dict, Optional, Any
from pydantic import BaseModel, Field


class HotspotAssessment(BaseModel):
    calibrated_hotspot_probability: float = Field(..., ge=0.0, le=1.0)
    operational_threshold: float = Field(default=0.20)
    is_hotspot: bool
    governance_claim: str


class ForecastHorizons(BaseModel):
    pm25_t_plus_1h: float
    pm25_t_plus_3h: float
    pm25_t_plus_6h: float
    uncertainty_lower_bound_p10: float
    uncertainty_upper_bound_p90: float


class ConfidenceBreakdown(BaseModel):
    overall_confidence: float = Field(..., ge=0.0, le=1.0)
    model_margin_confidence: float
    data_completeness_score: float
    spatial_coverage_confidence: float


class EvidenceSignal(BaseModel):
    category: str  # DIRECT_OBSERVATION, REMOTE_SENSING, GIS_CONTEXT, MONITORING_COVERAGE
    source: str
    metric: str
    value: Any
    attribution_note: str


class AeroSentinelInferencePayload(BaseModel):
    """
    The canonical payload emitted by the ML Intelligence Layer (F3)
    consumed by Spring Boot (PostGIS storage) and React (Hyperlocal Map & Dashboards).
    """
    timestamp: str
    h3_cell_id: str
    model_version: str = "v1.0.0"
    hotspot: HotspotAssessment
    forecast: ForecastHorizons
    confidence: ConfidenceBreakdown
    evidence: List[EvidenceSignal]