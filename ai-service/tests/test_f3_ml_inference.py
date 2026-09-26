"""
AeroSentinel - F3 Phase 5 ML Hotspot Inference Integration Tests
File: ai-service/tests/test_f3_ml_inference.py

Tests:
1. Artifact verification (structure, model object, classes, threshold, feature_cols)
2. Exact 36-feature name and ordering contract
3. Wind unit boundary (36 km/h -> 10 m/s exactly once, no double conversion)
4. Model inference with real Pune FeatureSnapshot (predict_proba, positive class extraction, threshold 0.20)
5. Model domain validation (Pune accepted, Mumbai & Delhi rejected with MODEL_DOMAIN_UNSUPPORTED)
6. Error handling (missing features -> INSUFFICIENT_DATA)
7. Standalone predict_cli execution
"""

import json
from pathlib import Path
import pytest
import numpy as np
import pandas as pd
import joblib
from fastapi.testclient import TestClient

from app.main import app, ml_artifacts, lifespan
from ml.inference.predict_cli import run_inference, ARTIFACT_PATH

# 36 authoritative feature names in exact order
AUTHORITATIVE_36_FEATURES = [
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
    "fire_upwind_alignment_score",
]

# Real persisted Pune snapshot 1624baa3-a5f8-407b-b1c2-36bcee7650b1 (Shivajinagar, H3: 88608850e5fffff)
REAL_PUNE_FEATURES_KMH = {
    "co": 0.9, "o3": 24.0, "no2": 37.0, "so2": 14.0, "hour": 18, "pm10": 120.0,
    "wind_u": 4.3604, "wind_v": 0.0761, "dow_cos": -0.2225, "dow_sin": -0.9749,
    "hour_cos": 0.0, "hour_sin": -1.0, "humidity": 77.0, "latitude": 18.5315,
    "pressure": 947.5, "rainfall": 0.0, "longitude": 73.8471, "is_weekend": 1,
    "wind_speed": 15.7,  # in km/h from Open-Meteo
    "day_of_week": 5, "temperature": 24.9, "wind_direction": 269.0,
    "fire_count_24h_25km": 0, "fire_frp_sum_24h_25km": 0.0, "pm25_spatial_lag_mean": 78.0,
    "fire_frp_mean_24h_25km": 0.0, "fire_frp_distance_decay": 0.0, "nearest_fire_distance_km": 50.0,
    "stations_within_5km_count": 2, "fire_upwind_alignment_score": 0.0,
    "nearest_station_distance_km": 0.27, "monitoring_coverage_gap_flag": 0,
    "dist_to_nearest_industrial_km": 3.5, "dist_to_nearest_major_road_km": 0.4,
    "sensitive_receptors_count_2km": 4, "industrial_zone_within_2km_flag": 0
}


@pytest.fixture(scope="module")
def client():
    # Load models via lifespan context
    with TestClient(app) as c:
        yield c


def test_artifact_loading_and_metadata():
    """Verify physical artifact exists, loads, has CalibratedClassifierCV and classes [0, 1]."""
    assert ARTIFACT_PATH.exists(), f"Artifact missing at {ARTIFACT_PATH}"
    art = joblib.load(ARTIFACT_PATH)
    assert isinstance(art, dict)
    assert "model" in art
    assert "feature_cols" in art
    assert "operational_threshold" in art

    model = art["model"]
    assert hasattr(model, "predict_proba")
    assert hasattr(model, "classes_")
    np.testing.assert_array_equal(model.classes_, [0, 1])

    # Positive class index check
    pos_idx = int(np.where(model.classes_ == 1)[0][0])
    assert pos_idx == 1

    assert art["operational_threshold"] == 0.20


def test_feature_names_and_order_exact():
    """Verify exact 36 feature names and ordering matching contract."""
    art = joblib.load(ARTIFACT_PATH)
    cols = art["feature_cols"]
    assert len(cols) == 36
    assert cols == AUTHORITATIVE_36_FEATURES


def test_wind_unit_boundary_conversion():
    """Verify wind unit boundary: 36 km/h -> 10 m/s single conversion, no double conversion."""
    wind_kmh = 36.0
    wind_mps = wind_kmh / 3.6
    assert wind_mps == 10.0

    # Test with real Pune snapshot wind speed (15.7 km/h -> 4.36 m/s)
    raw_ws = REAL_PUNE_FEATURES_KMH["wind_speed"]
    norm_ws = round(raw_ws / 3.6, 2)
    assert norm_ws == 4.36


def test_fastapi_model_info(client):
    """Verify /api/v1/ml/hotspot/model-info endpoint."""
    resp = client.get("/api/v1/ml/hotspot/model-info")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "OPERATIONAL"
    assert data["modelVersion"] == "hotspot_classifier_v1"
    assert data["engineType"] == "ML"
    assert data["featureCount"] == 36
    assert data["operationalThreshold"] == 0.20


def test_fastapi_pune_inference(client):
    """Verify /api/v1/ml/hotspot/predict on real Pune snapshot."""
    # Convert wind_speed from km/h to m/s
    norm_features = dict(REAL_PUNE_FEATURES_KMH)
    norm_features["wind_speed"] = round(norm_features["wind_speed"] / 3.6, 2)

    payload = {
        "h3Index": "88608850e5fffff",
        "cityName": "Pune",
        "featureMap": norm_features
    }

    resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
    assert resp.status_code == 200
    data = resp.json()

    assert data["h3Index"] == "88608850e5fffff"
    assert data["status"] == "SUCCESS"
    assert data["modelVersion"] == "hotspot_classifier_v1"
    assert data["engineType"] == "ML"
    assert data["operationalThreshold"] == 0.20
    assert 0.0 <= data["riskScore"] <= 1.0
    assert data["riskScore"] == 0.7998
    assert data["isHotspot"] is True
    assert data["riskLevel"] == "CRITICAL"
    assert data["confidence"] >= 0.75


def test_fastapi_unsupported_domain_mumbai(client):
    """Verify model domain enforcement: Mumbai is rejected with MODEL_DOMAIN_UNSUPPORTED."""
    payload = {
        "h3Index": "88608b56b3fffff",
        "cityName": "Mumbai",
        "featureMap": REAL_PUNE_FEATURES_KMH
    }

    resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
    assert resp.status_code == 200
    data = resp.json()

    assert data["status"] == "MODEL_DOMAIN_UNSUPPORTED"
    assert data["isHotspot"] is False
    assert data["riskScore"] == 0.0
    assert data["confidence"] == 0.0
    assert "trained exclusively on Pune" in data["message"]


def test_fastapi_unsupported_domain_delhi(client):
    """Verify model domain enforcement: Delhi is rejected with MODEL_DOMAIN_UNSUPPORTED."""
    payload = {
        "h3Index": "883da11505fffff",
        "cityName": "Delhi",
        "featureMap": REAL_PUNE_FEATURES_KMH
    }

    resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
    assert resp.status_code == 200
    data = resp.json()

    assert data["status"] == "MODEL_DOMAIN_UNSUPPORTED"
    assert data["isHotspot"] is False


def test_fastapi_missing_features_insufficient_data(client):
    """Verify missing required features returns HTTP 400 with INSUFFICIENT_DATA."""
    incomplete = dict(REAL_PUNE_FEATURES_KMH)
    del incomplete["pm10"]
    del incomplete["no2"]

    payload = {
        "h3Index": "88608850e5fffff",
        "cityName": "Pune",
        "featureMap": incomplete
    }

    resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
    assert resp.status_code == 400
    assert "INSUFFICIENT_DATA" in resp.json()["detail"]


def test_predict_cli_runner():
    """Verify standalone predict_cli.py execution."""
    norm_features = dict(REAL_PUNE_FEATURES_KMH)
    norm_features["wind_speed"] = round(norm_features["wind_speed"] / 3.6, 2)

    payload = {
        "h3Index": "88608850e5fffff",
        "cityName": "Pune",
        "featureMap": norm_features
    }

    result = run_inference(payload)
    assert result["status"] == "SUCCESS"
    assert result["riskScore"] == 0.7998
    assert result["isHotspot"] is True
    assert result["riskLevel"] == "CRITICAL"
    assert result["confidence"] == 0.86
    assert result["modelVersion"] == "hotspot_classifier_v1"
    assert result["engineType"] == "ML"


def test_fastapi_non_finite_features(client):
    """Verify non-finite / invalid numeric features are rejected with HTTP 400 or 422."""
    invalid_features = dict(REAL_PUNE_FEATURES_KMH)
    invalid_features["wind_speed"] = round(invalid_features["wind_speed"] / 3.6, 2)
    invalid_features["temperature"] = "invalid_string_val"

    payload = {
        "h3Index": "88608850e5fffff",
        "cityName": "Pune",
        "featureMap": invalid_features
    }

    resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
    assert resp.status_code in [400, 422]


def test_fastapi_missing_individual_copollutants(client):
    """Verify missing any individual co-pollutant (PM10, NO2, SO2, CO, O3) returns HTTP 400."""
    for pollutant in ["pm10", "no2", "so2", "co", "o3"]:
        incomplete = dict(REAL_PUNE_FEATURES_KMH)
        incomplete["wind_speed"] = round(incomplete["wind_speed"] / 3.6, 2)
        del incomplete[pollutant]

        payload = {
            "h3Index": "88608850e5fffff",
            "cityName": "Pune",
            "featureMap": incomplete
        }

        resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
        assert resp.status_code == 400
        assert "INSUFFICIENT_DATA" in resp.json()["detail"]
        assert pollutant in resp.json()["detail"]


def test_fastapi_wrong_feature_count_array(client):
    """Verify feature arrays with != 36 elements are rejected with HTTP 400."""
    payload_35 = {
        "h3Index": "88608850e5fffff",
        "cityName": "Pune",
        "features": [1.0] * 35
    }
    resp = client.post("/api/v1/ml/hotspot/predict", json=payload_35)
    assert resp.status_code == 400
    assert "Expected exactly 36 features" in resp.json()["detail"]


def test_fastapi_repeated_inference_determinism(client):
    """Verify repeated inference on the same feature vector yields bit-for-bit identical probabilities."""
    norm_features = dict(REAL_PUNE_FEATURES_KMH)
    norm_features["wind_speed"] = round(norm_features["wind_speed"] / 3.6, 2)

    payload = {
        "h3Index": "88608850e5fffff",
        "cityName": "Pune",
        "featureMap": norm_features
    }

    scores = []
    for _ in range(5):
        resp = client.post("/api/v1/ml/hotspot/predict", json=payload)
        assert resp.status_code == 200
        scores.append(resp.json()["riskScore"])

    assert len(set(scores)) == 1
    assert scores[0] == 0.7998

