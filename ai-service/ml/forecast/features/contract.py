"""
AeroSentinel - Production F4 Forecast Feature Contract & Schema
File: ai-service/ml/forecast/features/contract.py

Authoritative source of truth for the 36-feature contract:
ai-service/models/artifacts/forecast_regressors_v1.joblib
"""

from pathlib import Path
from typing import List, Dict, Any
import joblib

BASE_DIR = Path(__file__).resolve().parent.parent.parent.parent
ARTIFACT_PATH = BASE_DIR / "models" / "artifacts" / "forecast_regressors_v1.joblib"

F4_FEATURE_COUNT = 36
F4_SCHEMA_VERSION = "f4-features-v1"

# The authoritative 36-feature ordering
ORDERED_FEATURE_NAMES: List[str] = [
    "latitude",
    "longitude",
    "pm10",
    "no2",
    "so2",
    "co",
    "o3",
    "hour",
    "day_of_week",
    "is_weekend",
    "hour_sin",
    "hour_cos",
    "dow_sin",
    "dow_cos",
    "temperature",
    "humidity",
    "wind_speed",
    "wind_direction",
    "wind_u",
    "wind_v",
    "rainfall",
    "pressure",
    "pm25_spatial_lag_mean",
    "nearest_station_distance_km",
    "stations_within_5km_count",
    "monitoring_coverage_gap_flag",
    "dist_to_nearest_industrial_km",
    "dist_to_nearest_major_road_km",
    "sensitive_receptors_count_2km",
    "industrial_zone_within_2km_flag",
    "fire_count_24h_25km",
    "fire_frp_sum_24h_25km",
    "fire_frp_mean_24h_25km",
    "nearest_fire_distance_km",
    "fire_frp_distance_decay",
    "fire_upwind_alignment_score"
]


def verify_artifact_contract() -> Dict[str, Any]:
    """
    Programmatically verifies that forecast_regressors_v1.joblib exists,
    contains exactly 36 features, and that its feature_cols matches ORDERED_FEATURE_NAMES exactly.
    """
    if not ARTIFACT_PATH.exists():
        raise FileNotFoundError(f"Forecast artifact not found at {ARTIFACT_PATH}")

    artifact = joblib.load(ARTIFACT_PATH)
    if "feature_cols" not in artifact:
        raise ValueError("Corrupt artifact: 'feature_cols' key missing")

    artifact_cols = artifact["feature_cols"]
    if len(artifact_cols) != F4_FEATURE_COUNT:
        raise ValueError(
            f"Feature count mismatch: expected {F4_FEATURE_COUNT}, but artifact has {len(artifact_cols)}"
        )

    if artifact_cols != ORDERED_FEATURE_NAMES:
        raise ValueError(
            f"Feature order mismatch between code schema and joblib artifact.\n"
            f"Expected: {ORDERED_FEATURE_NAMES}\n"
            f"Artifact: {artifact_cols}"
        )

    return {
        "status": "PASS",
        "feature_count": len(artifact_cols),
        "feature_cols": artifact_cols,
        "models": list(artifact.get("models", {}).keys()),
        "algorithm": artifact.get("algorithm", "RandomForestRegressor")
    }
