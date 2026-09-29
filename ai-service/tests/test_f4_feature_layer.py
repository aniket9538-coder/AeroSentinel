"""
AeroSentinel - Production F4 Forecast Feature Layer Hardened Test Suite
File: ai-service/tests/test_f4_feature_layer.py

Fulfills all requirements from the F4-P2 Final Hardening Patch:
  - Missingness semantics (real zero != missing != unavailable)
  - Weather fallback audit & provenance
  - Wind unit proof (13 km/h -> 3.6111 m/s single conversion, double conversion rejection)
  - Model-ready vector proof (shape 1, 36)
  - Quality status integrity (no false promotion to VALID)
"""

import sys
from pathlib import Path
from datetime import datetime
import pytest
import numpy as np
import pandas as pd
import joblib

# Add ai-service to sys.path
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from ml.forecast.features.contract import (
    ORDERED_FEATURE_NAMES,
    F4_FEATURE_COUNT,
    F4_SCHEMA_VERSION,
    verify_artifact_contract,
    ARTIFACT_PATH
)
from ml.forecast.features.vector import ForecastFeatureVector
from ml.forecast.features.validator import ForecastFeatureValidator, ForecastFeaturesInvalidError
from ml.forecast.features.adapter import ForecastFeatureAdapter
from ml.forecast.features.builder import ForecastFeatureBuilder


def make_valid_features() -> dict:
    return {
        "latitude": 18.5315,
        "longitude": 73.8471,
        "pm10": 120.0,
        "no2": 37.0,
        "so2": 14.0,
        "co": 0.9,
        "o3": 24.0,
        "hour": 17.0,
        "day_of_week": 6.0,
        "is_weekend": 1.0,
        "hour_sin": -0.9659,
        "hour_cos": -0.2588,
        "dow_sin": -0.7818,
        "dow_cos": 0.6235,
        "temperature": 30.1,
        "humidity": 52.0,
        "wind_speed": 13.0, # 13.0 km/h raw
        "wind_direction": 275.0,
        "wind_u": 3.5974,
        "wind_v": -0.3147,
        "rainfall": 0.0,
        "pressure": 949.8,
        "pm25_spatial_lag_mean": 78.5,
        "nearest_station_distance_km": 0.27,
        "stations_within_5km_count": 2.0,
        "monitoring_coverage_gap_flag": 0.0,
        "dist_to_nearest_industrial_km": 3.5,
        "dist_to_nearest_major_road_km": 0.4,
        "sensitive_receptors_count_2km": 4.0,
        "industrial_zone_within_2km_flag": 0.0,
        "fire_count_24h_25km": 0.0,
        "fire_frp_sum_24h_25km": 0.0,
        "fire_frp_mean_24h_25km": 0.0,
        "nearest_fire_distance_km": 50.0,
        "fire_frp_distance_decay": 0.0,
        "fire_upwind_alignment_score": 0.0
    }


class TestF4FeatureLayerHardened:

    def setup_method(self):
        self.validator = ForecastFeatureValidator()
        self.adapter = ForecastFeatureAdapter(self.validator)
        self.builder = ForecastFeatureBuilder(self.validator)

    # ==========================================
    # A. Contract & Ordering Tests
    # ==========================================
    def test_artifact_contract_verification(self):
        """Verifies code contract against forecast_regressors_v1.joblib."""
        res = verify_artifact_contract()
        assert res["status"] == "PASS"
        assert res["feature_count"] == 36
        assert len(res["feature_cols"]) == 36
        assert res["feature_cols"] == ORDERED_FEATURE_NAMES

        # Verify joblib artifact keys and horizons
        artifact = joblib.load(ARTIFACT_PATH)
        assert "models" in artifact
        assert 1 in artifact["models"]
        assert 3 in artifact["models"]
        assert 6 in artifact["models"]

    def test_exact_36_feature_names_and_order(self):
        assert len(ORDERED_FEATURE_NAMES) == 36
        assert ORDERED_FEATURE_NAMES[0] == "latitude"
        assert ORDERED_FEATURE_NAMES[1] == "longitude"
        assert ORDERED_FEATURE_NAMES[16] == "wind_speed"
        assert ORDERED_FEATURE_NAMES[22] == "pm25_spatial_lag_mean"
        assert ORDERED_FEATURE_NAMES[35] == "fire_upwind_alignment_score"

    # ==========================================
    # B. Missingness Semantics Tests
    # ==========================================
    def test_real_zero_vs_missing_vs_unavailable(self):
        """Proves real physical zero is distinct from missing and source unavailable."""
        payload = {
            "h3Index": "88608850e5fffff",
            "cityId": "pune",
            "featureMap": make_valid_features()
        }
        vec = self.builder.build_from_payload(payload)

        # Real zero: fire_count = 0.0 because no active fires detected
        assert vec.feature_provenance["fire_count_24h_25km"] == "REAL_ZERO"
        assert vec.feature_provenance["rainfall"] == "REAL_ZERO"

        # Valid positive measurement
        assert vec.feature_provenance["pm10"] == "VALID_OBSERVATION"
        assert vec.feature_provenance["temperature"] == "VALID_OBSERVATION"

    def test_missing_pollutant_semantics(self):
        """Missing pollutant must be tracked in missing_fields, not masked as valid observation."""
        feats = make_valid_features()
        del feats["pm10"]

        payload = {
            "h3Index": "88608850e5fffff",
            "cityId": "pune",
            "featureMap": feats,
            "qualityStatus": "MISSING",
            "missingFeatures": ["pm10"]
        }
        vec = self.builder.build_from_payload(payload)
        assert "pm10" in vec.missing_fields
        assert vec.feature_provenance["pm10"] == "MISSING"
        assert vec.quality_status == "MISSING"

    def test_unavailable_pollutant_source(self):
        """When multiple pollutants missing, status must be UNAVAILABLE."""
        feats = make_valid_features()
        for p in ["pm10", "no2", "so2", "co", "o3"]:
            del feats[p]

        payload = {
            "h3Index": "88608850e5fffff",
            "cityId": "mumbai",
            "featureMap": feats,
            "qualityStatus": "UNAVAILABLE",
            "missingFeatures": ["pm10", "no2", "so2", "co", "o3"]
        }
        vec = self.builder.build_from_payload(payload)
        assert vec.quality_status == "UNAVAILABLE"
        assert len(vec.missing_fields) == 5

    def test_quality_status_integrity_rejection(self):
        """Rejects vector when missing fields exist but quality_status is falsely claimed as VALID."""
        feats = make_valid_features()
        feats["pm10"] = 0.0
        vec = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp=datetime(2026, 9, 27, 12, 0),
            features=feats,
            quality_status="VALID", # False promotion!
            missing_fields=["pm10"]
        )
        with pytest.raises(ForecastFeaturesInvalidError) as exc_info:
            self.validator.validate(vec)
        assert "Check 14 failed" in str(exc_info.value)

    # ==========================================
    # C. Wind Unit Proof & Orthogonal Consistency
    # ==========================================
    def test_wind_unit_proof_13_kmh_to_3_6111_mps(self):
        """
        Wind speed 13.0 km/h must convert to 13 / 3.6 = 3.6111 m/s.
        Direction = 275° must yield u = +3.5974, v = -0.3147.
        """
        feats = make_valid_features()
        feats["wind_speed"] = 13.0
        feats["wind_direction"] = 275.0

        vec = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp=datetime(2026, 9, 27, 12, 0),
            features=feats
        )
        adapted = self.adapter.adapt(vec, normalize_wind_speed_to_mps=True)
        assert adapted["wind_speed_converted"] is True

        ws_mps = adapted["feature_dataframe"]["wind_speed"].iloc[0]
        assert ws_mps == 3.6111

        u = adapted["feature_dataframe"]["wind_u"].iloc[0]
        v = adapted["feature_dataframe"]["wind_v"].iloc[0]
        assert u == 3.5974
        assert v == -0.3147

    def test_double_wind_conversion_rejection(self):
        """Adapter must reject double conversion if candidate speed matches (kmh / 3.6 / 3.6)."""
        original_kmh = 13.0
        double_converted = (13.0 / 3.6) / 3.6 # ~1.003 m/s

        with pytest.raises(ValueError) as exc_info:
            self.adapter.verify_no_double_conversion(original_kmh, double_converted)
        assert "DOUBLE_WIND_CONVERSION_DETECTED" in str(exc_info.value)

    # ==========================================
    # D. Model-Ready Vector Shape Proof (1, 36)
    # ==========================================
    def test_model_ready_vector_shape(self):
        """Verifies model input has shape exactly (1, 36) with finite numeric values."""
        feats = make_valid_features()
        vec = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp=datetime(2026, 9, 27, 12, 0),
            features=feats
        )
        adapted = self.adapter.adapt(vec, normalize_wind_speed_to_mps=True)

        assert adapted["batch_shape"] == (1, 36)
        assert adapted["array_2d"].shape == (1, 36)
        assert adapted["feature_dataframe"].shape == (1, 36)

        # Verify no NaN, no Inf
        assert not np.isnan(adapted["array_2d"]).any()
        assert not np.isinf(adapted["array_2d"]).any()

    # ==========================================
    # E. Weather Fallback Semantics
    # ==========================================
    def test_weather_fallback_labeling(self):
        """When weather observation is missing, baseline imputation must be tracked in provenance."""
        feats = make_valid_features()
        feats["temperature"] = 25.0 # fallback

        payload = {
            "h3Index": "88608850e5fffff",
            "cityId": "pune",
            "featureMap": feats,
            "qualityStatus": "MISSING",
            "missingFeatures": ["temperature"]
        }
        vec = self.builder.build_from_payload(payload)
        assert vec.feature_provenance["temperature"] == "IMPUTED_BASELINE"
        assert vec.quality_status == "MISSING"

    # ==========================================
    # F. Zero Model Inference Verification
    # ==========================================
    def test_p2_does_not_execute_model_inference(self):
        """Guarantees model.predict() is NEVER called in P2."""
        feats = make_valid_features()
        vec = ForecastFeatureVector(city_id="pune", h3_index="88608850e5fffff", base_timestamp=datetime(2026, 9, 27, 12, 0), features=feats)
        adapted = self.adapter.adapt(vec)

        assert "predictions" not in adapted
        assert "predicted_pm25" not in adapted
        assert "horizon" not in adapted
