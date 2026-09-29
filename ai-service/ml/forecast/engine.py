"""
AeroSentinel - Production Forecast Inference Engine (F4-P3)
File: ai-service/ml/forecast/engine.py

Executes multi-horizon PM2.5 forecast inference (T+1h, T+3h, T+6h) using the trained
Random Forest regressor models stored in forecast_regressors_v1.joblib.

Responsibilities:
  1. Programmatic artifact loading with structure, key, and schema validation.
  2. Input feature vector validation (shape (1, 36), exact order, finite values).
  3. Multi-horizon inference for horizons [1, 3, 6].
  4. Empirical residual interval generation (P10/P90) read directly from artifact.
  5. Physical lower-bound clamping (max(0.0, pred + p10)).
  6. Target timestamp derivation (T0 + h).
  7. Strict output contract with forecastConfidence = null.
"""

from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict, Any, List, Optional, Tuple, Union
import numpy as np
import pandas as pd
import joblib

from ml.forecast.features.contract import (
    ORDERED_FEATURE_NAMES,
    F4_FEATURE_COUNT,
    ARTIFACT_PATH
)
from ml.forecast.features.vector import ForecastFeatureVector
from ml.forecast.features.validator import ForecastFeatureValidator, ForecastFeaturesInvalidError
from ml.forecast.features.adapter import ForecastFeatureAdapter
from ml.forecast.features.builder import ForecastFeatureBuilder

SUPPORTED_HORIZONS = [1, 3, 6]
MODEL_VERSION = "forecast_regressors_v1"

_CACHED_ARTIFACT: Optional[Dict[str, Any]] = None


def load_forecast_artifact(
    artifact_path: Optional[Path] = None,
    force_reload: bool = False
) -> Dict[str, Any]:
    """
    Loads and validates the forecast_regressors_v1.joblib artifact.
    Caches the loaded object to prevent redundant disk I/O.
    """
    global _CACHED_ARTIFACT
    if _CACHED_ARTIFACT is not None and not force_reload:
        return _CACHED_ARTIFACT

    path = artifact_path or ARTIFACT_PATH
    if not path.exists():
        raise FileNotFoundError(f"FORECAST_MODEL_UNAVAILABLE: Artifact not found at {path}")

    try:
        artifact = joblib.load(path)
    except Exception as e:
        raise ValueError(f"FORECAST_MODEL_CONTRACT_INVALID: Failed to deserialize artifact: {e}")

    # Top-level type validation
    if not isinstance(artifact, dict):
        raise ValueError("FORECAST_MODEL_CONTRACT_INVALID: Artifact is not a dictionary")

    # Required keys validation
    required_keys = {"models", "feature_cols", "residuals", "algorithm"}
    missing_keys = required_keys - set(artifact.keys())
    if missing_keys:
        raise ValueError(f"FORECAST_MODEL_CONTRACT_INVALID: Missing required artifact keys: {missing_keys}")

    # Models dictionary validation
    models_dict = artifact["models"]
    if not isinstance(models_dict, dict):
        raise ValueError("FORECAST_MODEL_CONTRACT_INVALID: 'models' must be a dictionary")

    for h in SUPPORTED_HORIZONS:
        if h not in models_dict:
            raise ValueError(f"FORECAST_MODEL_CONTRACT_INVALID: Missing regressor model for horizon {h}")

    # Feature cols validation
    feature_cols = artifact["feature_cols"]
    if len(feature_cols) != F4_FEATURE_COUNT:
        raise ValueError(
            f"FORECAST_MODEL_CONTRACT_INVALID: Feature count mismatch. Expected {F4_FEATURE_COUNT}, got {len(feature_cols)}"
        )
    if feature_cols != ORDERED_FEATURE_NAMES:
        raise ValueError(
            "FORECAST_MODEL_CONTRACT_INVALID: Feature order mismatch between code schema and joblib artifact."
        )

    # Residuals validation
    residuals = artifact["residuals"]
    if not isinstance(residuals, dict):
        raise ValueError("FORECAST_MODEL_CONTRACT_INVALID: 'residuals' must be a dictionary")

    for h in SUPPORTED_HORIZONS:
        if h not in residuals or "p10" not in residuals[h] or "p90" not in residuals[h]:
            raise ValueError(f"FORECAST_MODEL_CONTRACT_INVALID: Missing residual P10/P90 for horizon {h}")

    _CACHED_ARTIFACT = artifact
    return artifact


class ForecastInferenceEngine:
    def __init__(
        self,
        artifact_path: Optional[Path] = None,
        validator: Optional[ForecastFeatureValidator] = None,
        adapter: Optional[ForecastFeatureAdapter] = None,
        builder: Optional[ForecastFeatureBuilder] = None
    ):
        self.artifact = load_forecast_artifact(artifact_path)
        self.validator = validator or ForecastFeatureValidator()
        self.adapter = adapter or ForecastFeatureAdapter(self.validator)
        self.builder = builder or ForecastFeatureBuilder(self.validator)

    def predict(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """
        Executes multi-horizon forecast inference for horizons [1, 3, 6].

        Input payload structure:
          - h3Index / h3_index (str, required)
          - cityId / city_id (str, optional, defaults to 'pune')
          - parentPredictionId / parent_prediction_id (str, optional)
          - featureSnapshotId / feature_snapshot_id (str, optional)
          - predictedAt / baseTimestamp / timestamp (str or datetime, optional)
          - features: list of 36 values OR dict mapping feature name to value
        """
        if not isinstance(payload, dict):
            raise ValueError("INVALID_INPUT: Payload must be a dictionary")

        # 1. Lineage & Spatial Context Extraction
        h3_index = str(payload.get("h3Index") or payload.get("h3_index") or "").strip()
        if not h3_index:
            raise ValueError("INVALID_INPUT: 'h3Index' is required")

        city_id = str(payload.get("cityId") or payload.get("city_id") or "pune").strip()
        parent_prediction_id = payload.get("parentPredictionId") or payload.get("parent_prediction_id")
        feature_snapshot_id = payload.get("featureSnapshotId") or payload.get("feature_snapshot_id")

        # 2. Base Timestamp T0 parsing
        raw_t0 = payload.get("predictedAt") or payload.get("baseTimestamp") or payload.get("timestamp") or payload.get("observedAt")
        if raw_t0 is None:
            t0 = datetime.now(timezone.utc)
        elif isinstance(raw_t0, str):
            try:
                t0 = pd.to_datetime(raw_t0, utc=True).to_pydatetime()
            except Exception as e:
                raise ValueError(f"INVALID_INPUT: Malformed timestamp '{raw_t0}': {e}")
        elif isinstance(raw_t0, datetime):
            t0 = raw_t0 if raw_t0.tzinfo is not None else raw_t0.replace(tzinfo=timezone.utc)
        else:
            raise ValueError(f"INVALID_INPUT: Unsupported timestamp type: {type(raw_t0)}")

        # 3. Model Compatibility & Input Adaptation
        # If payload already has 2D array or 1D array of length 36
        if "array_2d" in payload and isinstance(payload["array_2d"], np.ndarray):
            X = payload["array_2d"]
            if X.shape != (1, F4_FEATURE_COUNT):
                raise ValueError(f"INVALID_INPUT: Expected array_2d shape (1, {F4_FEATURE_COUNT}), got {X.shape}")
        elif "feature_dataframe" in payload and isinstance(payload["feature_dataframe"], pd.DataFrame):
            X = payload["feature_dataframe"]
            if X.shape != (1, F4_FEATURE_COUNT):
                raise ValueError(f"INVALID_INPUT: Expected dataframe shape (1, {F4_FEATURE_COUNT}), got {X.shape}")
        else:
            # Build and adapt vector via dedicated F4 feature layer
            vector = self.builder.build_from_payload(payload)
            adapted = self.adapter.adapt(vector, normalize_wind_speed_to_mps=False)
            X = adapted["feature_dataframe"]

        # Validate finite numeric values
        if isinstance(X, pd.DataFrame):
            if X.isna().any().any():
                raise ValueError("INVALID_INPUT: Feature input contains NaN")
            if not np.isfinite(X.to_numpy()).all():
                raise ValueError("INVALID_INPUT: Feature input contains non-finite values (Infinity)")
        elif isinstance(X, np.ndarray):
            if np.isnan(X).any():
                raise ValueError("INVALID_INPUT: Feature input contains NaN")
            if not np.isfinite(X).all():
                raise ValueError("INVALID_INPUT: Feature input contains non-finite values (Infinity)")

        # 4. Multi-Horizon Regressor Execution
        models = self.artifact["models"]
        residuals = self.artifact["residuals"]

        forecast_records: List[Dict[str, Any]] = []

        for h in SUPPORTED_HORIZONS:
            model = models[h]

            # Model prediction
            try:
                preds = model.predict(X)
                pred_val = float(preds[0])
            except Exception as e:
                raise RuntimeError(f"MODEL_INFERENCE_FAILED: Horizon T+{h}h prediction failed: {e}")

            if np.isnan(pred_val) or np.isinf(pred_val):
                raise ValueError(f"INVALID_MODEL_OUTPUT: Non-finite prediction for horizon {h}: {pred_val}")

            # Empirical residual interval calculation
            p10 = float(residuals[h]["p10"])
            p90 = float(residuals[h]["p90"])

            # Round prediction to 2 decimal places for consistent presentation and interval derivation
            pred_rounded = round(pred_val, 2)

            # Physical lower-bound clamping (PM2.5 cannot be negative)
            lower_bound = round(max(0.0, pred_rounded + p10), 2)
            upper_bound = round(pred_rounded + p90, 2)

            # Interval validity check
            if lower_bound > upper_bound:
                raise ValueError(
                    f"INVALID_INTERVAL: Lower bound ({lower_bound}) > Upper bound ({upper_bound}) for horizon {h}"
                )

            # Target timestamp calculation: T0 + h hours
            target_time = t0 + timedelta(hours=h)

            forecast_records.append({
                "horizonHours": h,
                "targetTime": target_time.isoformat(),
                "predictedPm25": pred_rounded,
                "lowerBound": lower_bound,
                "upperBound": upper_bound,
                "unit": "ug/m3"
            })

        # 5. Strict Output Contract
        return {
            "modelVersion": MODEL_VERSION,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "h3Index": h3_index,
            "cityId": city_id,
            "parentPredictionId": str(parent_prediction_id) if parent_prediction_id else None,
            "featureSnapshotId": str(feature_snapshot_id) if feature_snapshot_id else None,
            "status": "SUCCESS",
            "forecasts": forecast_records,
            "forecastConfidence": None
        }
