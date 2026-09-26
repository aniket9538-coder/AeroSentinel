"""
AeroSentinel - Phase 5 Configurable Alert Thresholds & Scoring Weights
File: ai-service/app/utils/alert_config.py
Fulfills Section 24 (Threshold Design) and Section 28 (Configurable Weighting).
"""

from pydantic import BaseModel, Field


class AlertScoringConfig(BaseModel):
    """Configurable scoring weights and state classification thresholds."""
    # State thresholds (calibrated against empirical multi-source PMR validation data)
    alert_candidate_threshold: float = Field(default=0.55)
    monitor_threshold: float = Field(default=0.30)
    
    # Coverage/Completeness gating
    min_completeness_for_alert: float = Field(default=0.40)
    
    # Sub-component weights (Sum = 1.0)
    weight_observation: float = Field(default=0.30)
    weight_ml_forecast: float = Field(default=0.25)
    weight_multi_source: float = Field(default=0.20)
    weight_spatial: float = Field(default=0.15)
    weight_temporal: float = Field(default=0.10)
    
    # Conflict penalties
    conflict_penalty_multiplier: float = Field(default=0.25)
    
    # Spatial clustering distance
    h3_clustering_k_rings: int = Field(default=1)

    scoring_version: str = "v1.0.0"


# Global singleton configuration instance
alert_config = AlertScoringConfig()