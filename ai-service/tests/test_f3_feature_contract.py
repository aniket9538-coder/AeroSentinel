"""
AeroSentinel - F3 Feature Contract Audit & Test Suite
File: ai-service/tests/test_f3_feature_contract.py

Validates all 20 required contract assertions against:
  - Trained model artifact: hotspot_classifier_v1.joblib
  - Master feature extractor: ml/features/feature_service.py
  - Preprocessing and canonical schema specifications
"""

from datetime import datetime, timedelta, timezone
from pathlib import Path
import sys
import math
import joblib
import numpy as np
import pandas as pd
import pytest

# Ensure ai-service root is on sys.path
ai_service_root = Path(__file__).resolve().parent.parent
if str(ai_service_root) not in sys.path:
    sys.path.insert(0, str(ai_service_root))

from ml.features.feature_service import (
    FEATURE_SCHEMA_VERSION,
    FEATURE_COUNT,
    ORDERED_FEATURE_NAMES,
    derive_wind_vectors,
    compute_leave_one_out_spatial_lag,
    compute_temporal_features,
    compute_monitoring_coverage,
    compute_fire_features,
    assemble_feature_vector,
    haversine_distance_km,
)
from preprocessing.satellite import extract_satellite_features_for_observation, clean_satellite_dataframe


ARTIFACT_PATH = ai_service_root / "models" / "artifacts" / "hotspot_classifier_v1.joblib"


# =========================================================================
# 1. Exact Feature Names, Count & Order against Trained Model Artifact
# =========================================================================

def test_01_feature_count():
    """Requirement 1: Exact feature count must equal 36."""
    assert FEATURE_COUNT == 36
    assert len(ORDERED_FEATURE_NAMES) == 36


def test_02_feature_names_and_order_match_joblib():
    """Requirement 2 & 3: Feature names and order match hotspot_classifier_v1.joblib exactly."""
    if not ARTIFACT_PATH.exists():
        pytest.skip("hotspot_classifier_v1.joblib not present in current environment")

    artifact = joblib.load(ARTIFACT_PATH)
    joblib_features = artifact["feature_cols"]

    assert len(joblib_features) == 36
    assert joblib_features == ORDERED_FEATURE_NAMES, f"Mismatch in feature order:\nExpected: {joblib_features}\nGot: {ORDERED_FEATURE_NAMES}"


def test_04_datatypes_numeric():
    """Requirement 4: Every feature in assembled vector must be strictly numeric."""
    sample_inputs = {
        "latitude": 18.5204,
        "longitude": 73.8567,
        "pm10": 85.0,
        "no2": 35.0,
        "so2": 12.0,
        "co": 1.2,
        "o3": 25.0,
        "temperature": 28.0,
        "humidity": 55.0,
        "wind_speed": 12.0,
        "wind_direction": 270.0,
        "rainfall": 0.0,
        "pressure": 954.3,
        "pm25": 50.0,
        "observed_at": datetime(2026, 9, 26, 6, 0, tzinfo=timezone.utc)
    }
    vec, missing, status = assemble_feature_vector(sample_inputs)
    assert len(vec) == 36
    for k, v in vec.items():
        assert isinstance(v, (int, float, np.integer, np.floating)), f"Feature {k} has non-numeric type {type(v)}"


# =========================================================================
# 5. Units & 6. Wind Conversion & 7. Wind Vectors
# =========================================================================

def test_05_06_07_wind_conversion_and_vector_derivation():
    """Requirement 5, 6, 7: Wind speed km/h to m/s conversion and meteorological vector derivation."""
    # 36 km/h = 10.0 m/s
    # Wind from East (90 deg) -> blowing towards West (u = -10 m/s, v = 0 m/s)
    u, v = derive_wind_vectors(36.0, 90.0)
    assert abs(u - (-10.0)) < 0.01
    assert abs(v - 0.0) < 0.01

    # Wind from North (0 deg) -> blowing towards South (u = 0 m/s, v = -10 m/s)
    u_n, v_n = derive_wind_vectors(36.0, 0.0)
    assert abs(u_n - 0.0) < 0.01
    assert abs(v_n - (-10.0)) < 0.01

    # Calm wind condition: speed < 0.2 m/s (0.5 km/h = 0.138 m/s < 0.2) -> u=0, v=0
    u_calm, v_calm = derive_wind_vectors(0.5, 180.0)
    assert u_calm == 0.0
    assert v_calm == 0.0


# =========================================================================
# 8. Spatial Lag Leave-One-Out (No Self-Leakage)
# =========================================================================

def test_08_spatial_lag_leave_one_out():
    """Requirement 8: Spatial lag must exclude station's own concentration to prevent leakage."""
    station_pm25 = [40.0, 60.0, 80.0]

    # Station 0 (own = 40.0): mean of [60.0, 80.0] = 70.0
    lag_0 = compute_leave_one_out_spatial_lag(station_pm25, self_index=0)
    assert lag_0 == 70.0

    # Station 1 (own = 60.0): mean of [40.0, 80.0] = 60.0
    lag_1 = compute_leave_one_out_spatial_lag(station_pm25, self_index=1)
    assert lag_1 == 60.0

    # Fallback when only 1 station exists: own value
    single_lag = compute_leave_one_out_spatial_lag([55.0], self_index=0)
    assert single_lag == 55.0


# =========================================================================
# 9 & 10. Monitoring Network Distance & Coverage Gap Rule
# =========================================================================

def test_09_10_monitoring_coverage_gap():
    """Requirement 9 & 10: Haversine distance and 7.0km coverage gap threshold."""
    station_lat, station_lon = 18.5314, 73.8446

    # Close network (< 7.0 km)
    close_coords = [
        (18.5314, 73.8446),  # self
        (18.5500, 73.8446),  # ~2.0 km
        (18.5600, 73.8446),  # ~3.1 km
    ]
    res_close = compute_monitoring_coverage(station_lat, station_lon, close_coords)
    assert res_close["nearest_station_distance_km"] <= 7.0
    assert res_close["monitoring_coverage_gap_flag"] == 0
    assert res_close["stations_within_5km_count"] == 3

    # Sparse network (> 7.0 km)
    distant_coords = [
        (18.5314, 73.8446),  # self
        (18.6314, 73.8446),  # ~11.1 km
    ]
    res_distant = compute_monitoring_coverage(station_lat, station_lon, distant_coords)
    assert res_distant["nearest_station_distance_km"] > 7.0
    assert res_distant["monitoring_coverage_gap_flag"] == 1
    assert res_distant["stations_within_5km_count"] == 1


# =========================================================================
# 11. Temporal Features (Solar Time Asia/Kolkata)
# =========================================================================

def test_11_temporal_features_cyclical():
    """Requirement 11: Diurnal and weekly cyclical temporal features."""
    # 2026-09-26 06:30 UTC -> 12:00 IST
    t_noon = datetime(2026, 9, 26, 6, 30, tzinfo=timezone.utc)
    feats_noon = compute_temporal_features(t_noon)
    assert feats_noon["hour"] == 12
    assert abs(feats_noon["hour_sin"] - 0.0) < 0.01
    assert abs(feats_noon["hour_cos"] - (-1.0)) < 0.01
    # 2026-09-26 is Saturday -> dow=5, is_weekend=1
    assert feats_noon["day_of_week"] == 5
    assert feats_noon["is_weekend"] == 1


# =========================================================================
# 12 & 13. NASA FIRMS Active Fire Features & Non-Leakage
# =========================================================================

def test_12_13_fire_features_and_no_future_leakage():
    """Requirement 12 & 13: 24h lookback, no future fire leakage, and zero fill on absence."""
    obs_time = datetime(2026, 9, 26, 12, 0, tzinfo=timezone.utc)
    obs_lat, obs_lon = 18.5204, 73.8567

    # Case A: Absence of fires -> legitimate domain zero
    f_empty = compute_fire_features(obs_lat, obs_lon, 2.5, 90.0, [], obs_time=obs_time)
    assert f_empty["fire_count_24h_25km"] == 0
    assert f_empty["fire_frp_sum_24h_25km"] == 0.0
    assert f_empty["fire_frp_distance_decay"] == 0.0
    assert f_empty["nearest_fire_distance_km"] == 50.0

    # Case B: Future fire must be excluded
    future_fire = {
        "latitude": 18.55,
        "longitude": 73.86,
        "frp": 25.0,
        "detected_at": obs_time + timedelta(hours=2)  # Future!
    }
    past_fire = {
        "latitude": 18.55,
        "longitude": 73.86,
        "frp": 15.0,
        "detected_at": obs_time - timedelta(hours=3)  # Past (valid)
    }
    f_leak_test = compute_fire_features(obs_lat, obs_lon, 2.5, 90.0, [future_fire, past_fire], obs_time=obs_time)
    # Only past fire counted
    assert f_leak_test["fire_count_24h_25km"] == 1
    assert f_leak_test["fire_frp_sum_24h_25km"] == 15.0


# =========================================================================
# 14, 15, 16. Sentinel-5P Satellite Lookback & Cloud Filtering
# =========================================================================

def test_14_15_16_satellite_features_and_cloud_filtering():
    """Requirement 14, 15, 16: Cloud fraction <= 0.30, 24h lookback, and no future leakage."""
    obs_time = datetime(2026, 9, 26, 12, 0, tzinfo=timezone.utc)

    raw_sat_records = [
        # Past cloud-free observation
        {
            "observed_at": obs_time - timedelta(hours=6),
            "latitude": 18.52,
            "longitude": 73.85,
            "cloud_fraction": 0.15,
            "tropospheric_no2": 0.00012,
            "so2_column": 0.00005,
            "co_column": 0.03,
            "aerosol_index": 0.8
        },
        # Past cloudy observation (cloud_fraction > 0.30 -> rejected)
        {
            "observed_at": obs_time - timedelta(hours=2),
            "latitude": 18.52,
            "longitude": 73.85,
            "cloud_fraction": 0.65,
            "tropospheric_no2": 0.00099,
        },
        # Future observation (must be strictly ignored)
        {
            "observed_at": obs_time + timedelta(hours=3),
            "latitude": 18.52,
            "longitude": 73.85,
            "cloud_fraction": 0.05,
            "tropospheric_no2": 0.00050,
        }
    ]

    sat_df = clean_satellite_dataframe(pd.DataFrame(raw_sat_records))
    extracted = extract_satellite_features_for_observation(obs_time, "88608850e5fffff", sat_df, lookback_hours=24)

    # Cloud-free observation selected, cloudy and future excluded
    assert extracted["satellite_no2_trop"] == 0.00012
    assert extracted["satellite_cloud_fraction"] == 0.15


# =========================================================================
# 17 & 18. GIS Proximity & Sensitive Receptors
# =========================================================================

def test_17_18_gis_features():
    """Requirement 17 & 18: GIS proximity baselines."""
    vec, _, _ = assemble_feature_vector({"latitude": 18.52, "longitude": 73.85})
    assert vec["dist_to_nearest_industrial_km"] == 3.50
    assert vec["dist_to_nearest_major_road_km"] == 0.40
    assert vec["sensitive_receptors_count_2km"] == 4
    assert vec["industrial_zone_within_2km_flag"] == 0


# =========================================================================
# 19. Explicit Missingness Handling
# =========================================================================

def test_19_missing_feature_handling():
    """Requirement 19: Missing co-pollutants tracked and explicitly flagged."""
    # Complete inputs
    comp_inputs = {
        "pm10": 60.0, "no2": 30.0, "so2": 10.0, "co": 1.0, "o3": 20.0
    }
    _, missing, status = assemble_feature_vector(comp_inputs)
    assert len(missing) == 0
    assert status == "VALID"

    # Missing co-pollutants (e.g. Mumbai station with only PM2.5)
    sparse_inputs = {}
    _, missing_sparse, status_sparse = assemble_feature_vector(sparse_inputs)
    assert len(missing_sparse) >= 5
    assert status_sparse in ["MISSING", "UNAVAILABLE"]


# =========================================================================
# 20. Deterministic Output
# =========================================================================

def test_20_deterministic_output():
    """Requirement 20: Deterministic feature vector generation."""
    inputs = {
        "latitude": 18.4529, "longitude": 73.8553,
        "pm10": 125.19, "no2": 35.27, "so2": 1.42, "co": 2.53, "o3": 0.42,
        "temperature": 26.5, "humidity": 46.0, "wind_speed": 6.8, "wind_direction": 87.0,
        "rainfall": 0.0, "pressure": 954.3, "pm25": 56.42,
        "observed_at": datetime(2026, 9, 26, 6, 0, tzinfo=timezone.utc)
    }
    vec1, _, _ = assemble_feature_vector(inputs)
    vec2, _, _ = assemble_feature_vector(inputs)
    assert vec1 == vec2
