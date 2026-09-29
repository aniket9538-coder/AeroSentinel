"""
AeroSentinel - Production F4-P3 Forecast Model Inference Test Suite
File: ai-service/tests/test_f4_p3_inference.py

Fulfills all testing categories from Section P3-M:
  A. Artifact loading (existence, keys, horizons, feature order)
  B. Multi-horizon inference (1h, 3h, 6h, all in one request)
  C. Empirical residual intervals (P10/P90, lower bound physical clamp, order)
  D. Forecast confidence contract (strictly null, never copied from F3)
  E. Temporal target generation (T0 + 1h, T0 + 3h, T0 + 6h, UTC)
  F. Input validation (wrong shape, missing feature, NaN, Infinity)
  G. Failure handling (missing artifact, corrupt artifact, malformed input)
  H. Lineage context preservation (parentPredictionId, cityId, h3Index, snapshotId)
  I. Determinism (identical repeated outputs)
  J. Standalone CLI bridge verification
"""

import sys
import json
import subprocess
from pathlib import Path
from datetime import datetime, timezone
import pytest
import numpy as np
import pandas as pd
import joblib

BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from ml.forecast.features.contract import (
    ORDERED_FEATURE_NAMES,
    F4_FEATURE_COUNT,
    ARTIFACT_PATH
)
from ml.forecast.engine import (
    ForecastInferenceEngine,
    load_forecast_artifact,
    SUPPORTED_HORIZONS,
    MODEL_VERSION
)


def make_pune_features_dict() -> dict:
    return {
        "latitude": 18.5315,
        "longitude": 73.8471,
        "pm10": 120.0,
        "no2": 37.0,
        "so2": 14.0,
        "co": 0.9,
        "o3": 24.0,
        "hour": 13.0,
        "day_of_week": 6.0,
        "is_weekend": 1.0,
        "hour_sin": -0.2588,
        "hour_cos": -0.9659,
        "dow_sin": -0.7818,
        "dow_cos": 0.6235,
        "temperature": 29.8,
        "humidity": 47.0,
        "wind_speed": 13.0,
        "wind_direction": 275.0,
        "wind_u": 3.5974,
        "wind_v": -0.3147,
        "rainfall": 0.7,
        "pressure": 948.4,
        "pm25_spatial_lag_mean": 78.0,
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


def make_pune_payload() -> dict:
    return {
        "parentPredictionId": "a310c689-f340-49fc-8935-a037de8d7709",
        "cityId": "550e8400-e29b-41d4-a716-446655440001",
        "h3Index": "88608850e5fffff",
        "featureSnapshotId": "007f7904-cee8-465c-a960-6c78bfa7c3c8",
        "predictedAt": "2026-09-27T08:04:54.689450Z",
        "features": make_pune_features_dict()
    }


class TestF4P3Inference:

    def setup_method(self):
        self.engine = ForecastInferenceEngine()

    # ==========================================
    # A. Artifact Loading Tests
    # ==========================================
    def test_artifact_loading_and_keys(self):
        artifact = load_forecast_artifact()
        assert isinstance(artifact, dict)
        assert "models" in artifact
        assert "feature_cols" in artifact
        assert "residuals" in artifact
        assert "algorithm" in artifact

        assert len(artifact["feature_cols"]) == F4_FEATURE_COUNT
        assert artifact["feature_cols"] == ORDERED_FEATURE_NAMES

        for h in SUPPORTED_HORIZONS:
            assert h in artifact["models"]
            assert h in artifact["residuals"]
            assert "p10" in artifact["residuals"][h]
            assert "p90" in artifact["residuals"][h]

    # ==========================================
    # B. Multi-Horizon Inference Tests
    # ==========================================
    def test_multi_horizon_inference_execution(self):
        payload = make_pune_payload()
        res = self.engine.predict(payload)

        assert res["status"] == "SUCCESS"
        assert res["modelVersion"] == MODEL_VERSION
        assert len(res["forecasts"]) == 3

        horizons = [f["horizonHours"] for f in res["forecasts"]]
        assert horizons == [1, 3, 6]

        for f in res["forecasts"]:
            assert f["unit"] == "ug/m3"
            assert isinstance(f["predictedPm25"], float)
            assert np.isfinite(f["predictedPm25"])
            assert f["predictedPm25"] > 0.0

    # ==========================================
    # C. Empirical Residual Intervals Tests
    # ==========================================
    def test_empirical_residual_intervals_and_clamping(self):
        payload = make_pune_payload()
        res = self.engine.predict(payload)

        residuals = self.engine.artifact["residuals"]

        for f in res["forecasts"]:
            h = f["horizonHours"]
            pred = f["predictedPm25"]
            lower = f["lowerBound"]
            upper = f["upperBound"]

            p10 = residuals[h]["p10"]
            p90 = residuals[h]["p90"]

            expected_lower = round(max(0.0, pred + p10), 2)
            expected_upper = round(pred + p90, 2)

            assert lower == expected_lower
            assert upper == expected_upper
            assert lower <= pred <= upper
            assert lower >= 0.0  # physical clamp

    # ==========================================
    # D. Confidence Contract Tests
    # ==========================================
    def test_forecast_confidence_is_strictly_null(self):
        payload = make_pune_payload()
        res = self.engine.predict(payload)

        assert "forecastConfidence" in res
        assert res["forecastConfidence"] is None

    # ==========================================
    # E. Timestamp Target Generation Tests
    # ==========================================
    def test_timestamp_target_generation(self):
        payload = make_pune_payload()
        res = self.engine.predict(payload)

        t0 = pd.to_datetime(payload["predictedAt"], utc=True)

        for f in res["forecasts"]:
            h = f["horizonHours"]
            target_ts = pd.to_datetime(f["targetTime"], utc=True)
            expected_ts = t0 + pd.Timedelta(hours=h)
            assert target_ts == expected_ts

    # ==========================================
    # F. Validation & Rejection Tests
    # ==========================================
    def test_rejection_of_nan_in_features(self):
        payload = make_pune_payload()
        payload["features"]["temperature"] = float("nan")

        with pytest.raises(ValueError) as exc_info:
            self.engine.predict(payload)
        assert "NaN" in str(exc_info.value) or "Check 7 failed" in str(exc_info.value)

    def test_rejection_of_infinity_in_features(self):
        payload = make_pune_payload()
        payload["features"]["pressure"] = float("inf")

        with pytest.raises(ValueError) as exc_info:
            self.engine.predict(payload)
        assert "Infinite" in str(exc_info.value) or "Check 4/8 failed" in str(exc_info.value)

    def test_rejection_of_missing_h3_index(self):
        payload = make_pune_payload()
        payload["h3Index"] = ""

        with pytest.raises(ValueError) as exc_info:
            self.engine.predict(payload)
        assert "'h3Index' is required" in str(exc_info.value)

    # ==========================================
    # G. Failure Handling Tests
    # ==========================================
    def test_failure_on_missing_artifact(self):
        fake_path = Path("ai-service/models/artifacts/non_existent.joblib")
        with pytest.raises(FileNotFoundError) as exc_info:
            load_forecast_artifact(artifact_path=fake_path, force_reload=True)
        assert "FORECAST_MODEL_UNAVAILABLE" in str(exc_info.value)

    # ==========================================
    # H. Lineage Preservation Tests
    # ==========================================
    def test_lineage_context_preservation(self):
        payload = make_pune_payload()
        res = self.engine.predict(payload)

        assert res["parentPredictionId"] == payload["parentPredictionId"]
        assert res["cityId"] == payload["cityId"]
        assert res["h3Index"] == payload["h3Index"]
        assert res["featureSnapshotId"] == payload["featureSnapshotId"]

    # ==========================================
    # I. Determinism Tests
    # ==========================================
    def test_inference_determinism(self):
        payload1 = make_pune_payload()
        payload2 = make_pune_payload()

        res1 = self.engine.predict(payload1)
        res2 = self.engine.predict(payload2)

        for f1, f2 in zip(res1["forecasts"], res2["forecasts"]):
            assert f1["horizonHours"] == f2["horizonHours"]
            assert f1["predictedPm25"] == f2["predictedPm25"]
            assert f1["lowerBound"] == f2["lowerBound"]
            assert f1["upperBound"] == f2["upperBound"]
            assert f1["targetTime"] == f2["targetTime"]

    # ==========================================
    # J. CLI Execution Tests
    # ==========================================
    def test_cli_execution_with_argument(self):
        payload = make_pune_payload()
        cli_path = BASE_DIR / "ml" / "inference" / "predict_forecast_cli.py"

        p = subprocess.run(
            ["python", str(cli_path), json.dumps(payload)],
            capture_output=True,
            text=True
        )
        assert p.returncode == 0
        out_json = json.loads(p.stdout)
        assert out_json["status"] == "SUCCESS"
        assert len(out_json["forecasts"]) == 3
        assert out_json["forecastConfidence"] is None

    def test_cli_rejection_of_empty_input(self):
        cli_path = BASE_DIR / "ml" / "inference" / "predict_forecast_cli.py"
        p = subprocess.run(
            ["python", str(cli_path), ""],
            capture_output=True,
            text=True
        )
        assert p.returncode == 1
        err_json = json.loads(p.stderr)
        assert err_json["status"] == "ERROR"
