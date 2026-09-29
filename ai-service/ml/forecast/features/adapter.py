"""
AeroSentinel - Production F4 Forecast Feature Adapter
File: ai-service/ml/forecast/features/adapter.py

Adapts ForecastFeatureVector into model-ready arrays and DataFrames matching
the input requirements (1, 36) of forecast_regressors_v1.joblib without performing model inference.
"""

import math
from typing import Dict, Any, Optional
import numpy as np
import pandas as pd

from ml.forecast.features.contract import ORDERED_FEATURE_NAMES, F4_FEATURE_COUNT
from ml.forecast.features.vector import ForecastFeatureVector
from ml.forecast.features.validator import ForecastFeatureValidator


class ForecastFeatureAdapter:
    def __init__(self, validator: Optional[ForecastFeatureValidator] = None):
        self.validator = validator or ForecastFeatureValidator()

    def verify_no_double_conversion(self, original_kmh: float, candidate_mps: float) -> None:
        """
        Rejects candidate wind speed if it matches double-conversion (km/h -> m/s -> m/s).
        """
        single_conv = original_kmh / 3.6
        double_conv = single_conv / 3.6
        if abs(candidate_mps - double_conv) < 0.05:
            raise ValueError(
                f"DOUBLE_WIND_CONVERSION_DETECTED: Candidate speed {candidate_mps} m/s "
                f"matches double conversion from {original_kmh} km/h (expected single {single_conv:.4f} m/s)"
            )

    def adapt(
        self,
        vector: ForecastFeatureVector,
        normalize_wind_speed_to_mps: bool = False
    ) -> Dict[str, Any]:
        """
        Validates and adapts a ForecastFeatureVector for model input.

        Args:
            vector: ForecastFeatureVector instance
            normalize_wind_speed_to_mps: if True, converts wind_speed from km/h to m/s (/ 3.6)
                                         and updates orthogonal u/v components for exact physical alignment.

        Returns:
            Dict containing:
              - h3_index: str
              - city_id: str
              - base_timestamp: str
              - feature_snapshot_id: Optional[str]
              - quality_status: str
              - missing_fields: List[str]
              - ordered_values: np.ndarray (shape=(36,), dtype=float64)
              - array_2d: np.ndarray (shape=(1, 36), dtype=float64)
              - feature_dataframe: pd.DataFrame (shape=(1, 36))
              - wind_speed_converted: bool
              - batch_shape: tuple = (1, 36)
        """
        # 1. Validate vector
        self.validator.validate(vector)

        # 2. Extract ordered array
        raw_values = vector.ordered_values.copy()
        feature_map = dict(vector.features)
        wind_converted = False

        if normalize_wind_speed_to_mps:
            ws_idx = ORDERED_FEATURE_NAMES.index("wind_speed")
            wd_idx = ORDERED_FEATURE_NAMES.index("wind_direction")
            wu_idx = ORDERED_FEATURE_NAMES.index("wind_u")
            wv_idx = ORDERED_FEATURE_NAMES.index("wind_v")

            raw_kmh = raw_values[ws_idx]
            speed_mps = round(raw_kmh / 3.6, 4)
            raw_values[ws_idx] = speed_mps
            feature_map["wind_speed"] = speed_mps
            wind_converted = True

            # Align orthogonal wind vectors strictly with the single-converted m/s speed
            dir_deg = raw_values[wd_idx]
            if speed_mps >= 0.2:
                rad = math.radians(dir_deg)
                u = round(-speed_mps * math.sin(rad), 4)
                v = round(-speed_mps * math.cos(rad), 4)
                raw_values[wu_idx] = u
                raw_values[wv_idx] = v
                feature_map["wind_u"] = u
                feature_map["wind_v"] = v

        # 3. Build single-row DataFrame matching artifact columns
        df = pd.DataFrame([feature_map])[ORDERED_FEATURE_NAMES].fillna(0.0)

        # Final safety check: no NaN, no Inf, exact shape (1, 36)
        if df.isna().any().any():
            raise ValueError(f"Adapted DataFrame contains NaN for cell {vector.h3_index}")

        array_2d = raw_values.reshape(1, -1)

        return {
            "h3_index": vector.h3_index,
            "city_id": vector.city_id,
            "base_timestamp": vector.base_timestamp.isoformat() if hasattr(vector.base_timestamp, "isoformat") else str(vector.base_timestamp),
            "feature_snapshot_id": vector.feature_snapshot_id,
            "quality_status": vector.quality_status,
            "missing_fields": list(vector.missing_fields),
            "ordered_values": raw_values,
            "array_2d": array_2d,
            "batch_shape": array_2d.shape,
            "feature_dataframe": df,
            "wind_speed_converted": wind_converted
        }
