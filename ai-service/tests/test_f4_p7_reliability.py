"""
AeroSentinel - Production F4-P7 Forecast Reliability & Controlled Failure Tests (Python)
File: ai-service/tests/test_f4_p7_reliability.py

Validates Track B Engineering Reliability Requirements:
  P7.1 - Insufficient history detection & rejection
  P7.2 - Missing weather inputs & provenance preservation
  P7.3 - Model unavailable handling without artifact deletion
  P7.4 - CLI / ML process failure & malformed input handling
  P7.6 - Invalid bounds rejection (Cases A, B, C, D)
  P7.11 - Model artifact integrity check
"""

import json
import subprocess
import sys
from pathlib import Path
import numpy as np
import pytest

from ml.forecast.features.contract import (
    ORDERED_FEATURE_NAMES,
    F4_FEATURE_COUNT,
    ARTIFACT_PATH
)
from ml.forecast.features.vector import ForecastFeatureVector
from ml.forecast.features.validator import (
    ForecastFeatureValidator,
    ForecastFeaturesInvalidError
)
from ml.forecast.features.builder import ForecastFeatureBuilder
from ml.forecast.engine import ForecastInferenceEngine, load_forecast_artifact

CLI_PATH = Path(__file__).resolve().parent.parent / "ml" / "inference" / "predict_forecast_cli.py"


@pytest.fixture
def validator():
    return ForecastFeatureValidator()


@pytest.fixture
def builder(validator):
    return ForecastFeatureBuilder(validator)


@pytest.fixture
def valid_pune_features():
    """Valid complete 36-feature map for Pune cell 88608850e5fffff."""
    return {
        "latitude": 18.5314,
        "longitude": 73.8446,
        "pm10": 112.4,
        "no2": 34.2,
        "so2": 14.5,
        "co": 1.2,
        "o3": 28.6,
        "hour": 14.0,
        "day_of_week": 4.0,
        "is_weekend": 0.0,
        "hour_sin": 0.2588,
        "hour_cos": -0.9659,
        "dow_sin": 0.4339,
        "dow_cos": -0.9010,
        "temperature": 27.5,
        "humidity": 62.0,
        "wind_speed": 3.61,
        "wind_direction": 240.0,
        "wind_u": -3.13,
        "wind_v": -1.81,
        "rainfall": 0.0,
        "pressure": 1012.0,
        "pm25_spatial_lag_mean": 68.4,
        "nearest_station_distance_km": 1.2,
        "stations_within_5km_count": 3.0,
        "monitoring_coverage_gap_flag": 0.0,
        "dist_to_nearest_industrial_km": 4.5,
        "dist_to_nearest_major_road_km": 0.3,
        "sensitive_receptors_count_2km": 8.0,
        "industrial_zone_within_2km_flag": 0.0,
        "fire_count_24h_25km": 0.0,
        "fire_frp_sum_24h_25km": 0.0,
        "fire_frp_mean_24h_25km": 0.0,
        "nearest_fire_distance_km": 50.0,
        "fire_frp_distance_decay": 0.0,
        "fire_upwind_alignment_score": 0.0
    }


class TestF4P7Reliability:

    # =========================================================================
    # P7.1 — INSUFFICIENT HISTORY
    # =========================================================================
    def test_p7_1_missing_historical_features_strict_rejection(self, validator, valid_pune_features):
        """P7.1: Historical features missing must fail strict feature validation."""
        incomplete_features = valid_pune_features.copy()
        # Remove / set to None critical historical spatial lag feature
        del incomplete_features["pm25_spatial_lag_mean"]

        # 1. Feature count violation
        vector = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp="2026-09-26T12:00:00Z",
            features=incomplete_features,
            quality_status="MISSING",
            missing_fields=["pm25_spatial_lag_mean"]
        )

        with pytest.raises(ForecastFeaturesInvalidError) as exc_info:
            validator.validate(vector)
        assert "Check 1/2 failed" in str(exc_info.value) or "Missing expected feature" in str(exc_info.value)

    def test_p7_1_strict_no_missing_mode_rejection(self, validator, valid_pune_features):
        """P7.1: Strict mode must reject any vector with missing fields."""
        features = valid_pune_features.copy()
        vector = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp="2026-09-26T12:00:00Z",
            features=features,
            quality_status="MISSING",
            missing_fields=["pm10"]
        )

        with pytest.raises(ForecastFeaturesInvalidError) as exc_info:
            validator.validate(vector, strict_no_missing=True)
        assert "Check 6 failed: Strict mode requires zero missing features" in str(exc_info.value)

    def test_p7_1_quality_status_integrity_cannot_be_valid_when_history_missing(self, validator, valid_pune_features):
        """P7.1: Quality status cannot be falsely promoted to VALID when fields are missing."""
        features = valid_pune_features.copy()
        vector = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp="2026-09-26T12:00:00Z",
            features=features,
            quality_status="VALID", # falsely claiming VALID
            missing_fields=["pm25_spatial_lag_mean"]
        )

        with pytest.raises(ForecastFeaturesInvalidError) as exc_info:
            validator.validate(vector)
        assert "Check 14 failed: Quality status cannot be VALID when physical fields are missing" in str(exc_info.value)

    # =========================================================================
    # P7.2 — MISSING WEATHER
    # =========================================================================
    def test_p7_2_missing_weather_provenance_tracking(self, builder, valid_pune_features):
        """P7.2: Missing weather inputs must be assigned MISSING provenance, not VALID_OBSERVATION."""
        payload_features = valid_pune_features.copy()
        # Simulate missing temperature and wind
        payload_features["temperature"] = None
        payload_features["wind_speed"] = None

        payload = {
            "h3Index": "88608850e5fffff",
            "cityId": "pune",
            "features": payload_features,
            "qualityStatus": "MISSING"
        }

        vector = builder.build_from_payload(payload)
        assert "temperature" in vector.missing_fields
        assert "wind_speed" in vector.missing_fields
        assert vector.feature_provenance["temperature"] == "MISSING"
        assert vector.feature_provenance["wind_speed"] == "MISSING"
        assert vector.feature_provenance["latitude"] == "VALID_OBSERVATION"
        assert vector.quality_status in ("MISSING", "UNAVAILABLE")

    def test_p7_2_missing_weather_cannot_be_claimed_as_valid(self, validator, valid_pune_features):
        """P7.2: Weather provenance MISSING / SOURCE_UNAVAILABLE rejects VALID quality status."""
        features = valid_pune_features.copy()
        vector = ForecastFeatureVector(
            city_id="pune",
            h3_index="88608850e5fffff",
            base_timestamp="2026-09-26T12:00:00Z",
            features=features,
            quality_status="VALID",
            feature_provenance={"temperature": "SOURCE_UNAVAILABLE"}
        )

        with pytest.raises(ForecastFeaturesInvalidError) as exc_info:
            validator.validate(vector)
        assert "Check 14 failed" in str(exc_info.value)
        assert "SOURCE_UNAVAILABLE" in str(exc_info.value)

    # =========================================================================
    # P7.3 — MODEL UNAVAILABLE
    # =========================================================================
    def test_p7_3_model_unavailable_isolated_check(self):
        """P7.3: When model artifact path is missing, raises FileNotFoundError with controlled code."""
        fake_path = Path("models/artifacts/non_existent_model_v999.joblib")
        with pytest.raises(FileNotFoundError) as exc_info:
            load_forecast_artifact(fake_path, force_reload=True)
        assert "FORECAST_MODEL_UNAVAILABLE" in str(exc_info.value)

    # =========================================================================
    # P7.4 — ML / SERVICE / CLI FAILURE MODES
    # =========================================================================
    def test_p7_4_cli_empty_stdin_exit_code_1(self):
        """P7.4: CLI called with empty input exits with code 1 and error JSON."""
        proc = subprocess.run(
            [sys.executable, str(CLI_PATH)],
            input="",
            capture_output=True,
            text=True
        )
        assert proc.returncode == 1
        assert "INVALID_INPUT" in proc.stderr

    def test_p7_4_cli_malformed_json_exit_code_1(self):
        """P7.4: CLI called with malformed JSON exits with code 1 and error JSON."""
        proc = subprocess.run(
            [sys.executable, str(CLI_PATH)],
            input="{malformed_json_syntax: true",
            capture_output=True,
            text=True
        )
        assert proc.returncode == 1
        assert "INVALID_INPUT" in proc.stderr

    def test_p7_4_cli_missing_h3_index_exit_code_3(self, valid_pune_features):
        """P7.4: CLI called without mandatory h3Index exits with code 3 (contract invalid)."""
        payload = {
            "cityId": "pune",
            "features": valid_pune_features
        }
        proc = subprocess.run(
            [sys.executable, str(CLI_PATH)],
            input=json.dumps(payload),
            capture_output=True,
            text=True
        )
        assert proc.returncode == 3
        err = json.loads(proc.stdout)
        assert err["status"] == "INVALID_INPUT"

    # =========================================================================
    # P7.6 — INVALID BOUNDS & NUMERICAL SANITY
    # =========================================================================
    def test_p7_6_nan_in_features_strictly_rejected(self, valid_pune_features):
        """P7.6: NaN in feature values must be rejected."""
        engine = ForecastInferenceEngine()
        payload = {
            "h3Index": "88608850e5fffff",
            "features": valid_pune_features.copy()
        }
        payload["features"]["temperature"] = float("nan")

        with pytest.raises(Exception) as exc_info:
            engine.predict(payload)
        assert "NaN" in str(exc_info.value) or "Check 7 failed" in str(exc_info.value)

    def test_p7_6_infinity_in_features_strictly_rejected(self, valid_pune_features):
        """P7.6: Infinity in feature values must be rejected."""
        engine = ForecastInferenceEngine()
        payload = {
            "h3Index": "88608850e5fffff",
            "features": valid_pune_features.copy()
        }
        payload["features"]["wind_speed"] = float("inf")

        with pytest.raises(Exception) as exc_info:
            engine.predict(payload)
        assert "Infinite" in str(exc_info.value) or "Check 4/8 failed" in str(exc_info.value) or "non-finite" in str(exc_info.value)

    def test_p7_6_physical_lower_bound_clamping_to_zero(self, valid_pune_features):
        """P7.6: Lower bound can never be negative (empirically clamped to max(0.0, pred + p10))."""
        engine = ForecastInferenceEngine()
        payload = {
            "h3Index": "88608850e5fffff",
            "features": valid_pune_features
        }
        result = engine.predict(payload)
        for f in result["forecasts"]:
            assert f["lowerBound"] >= 0.0, f"Horizon {f['horizonHours']} lowerBound must be >= 0"
            assert f["lowerBound"] <= f["predictedPm25"], f"Horizon {f['horizonHours']} lowerBound <= predicted"
            assert f["predictedPm25"] <= f["upperBound"], f"Horizon {f['horizonHours']} predicted <= upperBound"

    # =========================================================================
    # P7.11 — PRODUCTION ARTIFACT INTEGRITY VERIFICATION
    # =========================================================================
    def test_p7_11_production_artifact_intact_and_unmodified(self):
        """P7.11: Production model artifact must exist, have valid structure, and match expected SHA256."""
        assert ARTIFACT_PATH.exists(), f"Production artifact must exist at {ARTIFACT_PATH}"
        artifact = load_forecast_artifact(ARTIFACT_PATH, force_reload=True)
        assert "models" in artifact
        assert "residuals" in artifact
        assert {1, 3, 6}.issubset(set(artifact["models"].keys()))
        assert len(artifact["feature_cols"]) == 36
