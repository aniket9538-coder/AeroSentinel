"""
AeroSentinel - F1 Data Foundation Unit Test Suite
File: tests/ai/test_f1_foundation.py

Tests temporal alignment, spatial coordinate validation, H3 resolution 8 binding,
meteorological wind vector decomposition, pollutant quality flagging, and
leakage-free chronological dataset splitting.
"""

from datetime import datetime, timezone
import numpy as np
import pandas as pd
import pytest

from preprocessing.temporal_alignment import normalize_timestamp, align_to_hourly_bin
from preprocessing.spatial_alignment import validate_coordinates, bind_h3_cell
from preprocessing.normalization import classify_pollutant_value, decompose_wind
from preprocessing.pipeline import (
    align_air_and_weather,
    generate_forecast_targets,
    split_chronologically,
)
from app.schemas.canonical import QualityFlag, EnvironmentalObservation


# =====================================================================
# 1. TEMPORAL & SPATIAL FOUNDATION TESTS
# =====================================================================

def test_normalize_timestamp_utc_conversion():
    ts = normalize_timestamp("2026-03-01 12:00:00", source_tz="Asia/Kolkata")
    assert ts is not None
    assert str(ts.tzinfo) == "UTC"
    assert ts.hour == 6  # 12:00 IST is 06:30 UTC -> 06:00 UTC floored


def test_spatial_validation_and_h3():
    valid, status = validate_coordinates(19.0760, 72.8777, bbox=None)
    assert valid is True
    assert status == "VALID"
    
    cell = bind_h3_cell(19.0760, 72.8777, resolution=8)
    assert cell is not None
    assert isinstance(cell, str)
    assert len(cell) == 15


def test_spatial_invalid_coordinates():
    # Out of boundary latitudes / longitudes return 'PHYSICALLY_IMPOSSIBLE_COORDINATES'
    valid_lat, status_lat = validate_coordinates(95.0, 72.8777)
    assert valid_lat is False
    assert status_lat == "PHYSICALLY_IMPOSSIBLE_COORDINATES"

    valid_lon, status_lon = validate_coordinates(19.0760, 195.0)
    assert valid_lon is False
    assert status_lon == "PHYSICALLY_IMPOSSIBLE_COORDINATES"


# =====================================================================
# 2. NORMALIZATION & METEOROLOGY TESTS
# =====================================================================

def test_pollutant_quality_flags():
    flag, val = classify_pollutant_value(45.2, "pm25")
    assert flag == QualityFlag.VALID
    assert val == 45.2

    # Negative sensor calibration error
    flag_inv, val_inv = classify_pollutant_value(-10.0, "pm25")
    assert flag_inv == QualityFlag.INVALID
    assert val_inv is None


def test_pollutant_extreme_spikes_preservation():
    # Extreme pollution spikes (> 500 ug/m3) must be flagged SUSPECT, not clipped
    flag_spike, val_spike = classify_pollutant_value(580.0, "pm25")
    assert flag_spike == QualityFlag.SUSPECT
    assert val_spike == 580.0


def test_wind_vector_decomposition():
    # 10 m/s from North (0 deg) -> U=0, V=-10
    u, v = decompose_wind(10.0, 0.0)
    assert abs(u - 0.0) < 1e-4
    assert abs(v - (-10.0)) < 1e-4

    # 10 m/s from East (90 deg) -> U=-10, V=0
    u_east, v_east = decompose_wind(10.0, 90.0)
    assert abs(u_east - (-10.0)) < 1e-4
    assert abs(v_east - 0.0) < 1e-4

    # Calm wind (< 0.2 m/s) -> U=0, V=0
    u_calm, v_calm = decompose_wind(0.1, 180.0)
    assert u_calm == 0.0 and v_calm == 0.0

    # Missing direction with non-calm wind
    u_none, v_none = decompose_wind(5.0, None)
    assert u_none == 0.0 and v_none == 0.0


# =====================================================================
# 3. SPATIOTEMPORAL ALIGNMENT & LEAKAGE PREVENTION TESTS
# =====================================================================

def test_cyclical_features_alignment():
    # Aligned dataset must contain local cyclical temporal features
    air_df = pd.DataFrame({
        "observed_at": [datetime(2026, 3, 1, 6, 0, tzinfo=timezone.utc)],  # 11:30 IST
        "hourly_bin": [pd.Timestamp("2026-03-01 06:00:00", tz="UTC")],
        "city_id": ["city-pune"],
        "station_id": ["ST_01"],
        "pm25_clean": [55.0]
    })
    weather_df = pd.DataFrame({
        "hourly_bin": [pd.Timestamp("2026-03-01 06:00:00", tz="UTC")],
        "city_id": ["city-pune"],
        "temperature": [28.0],
        "humidity": [60.0]
    })
    aligned = align_air_and_weather(air_df, weather_df)
    assert "hour_sin" in aligned.columns
    assert "hour_cos" in aligned.columns
    assert "dow_sin" in aligned.columns
    assert "dow_cos" in aligned.columns
    assert aligned["temperature"].iloc[0] == 28.0


def test_forecast_targets_shift_leak_prevention():
    dates = pd.date_range(start="2026-03-01 00:00:00", periods=10, freq="h", tz="UTC")
    df = pd.DataFrame({
        "station_id": ["ST_01"] * 10,
        "hourly_bin": dates,
        "pm25_clean": [10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0]
    })
    targeted = generate_forecast_targets(df, horizons=[1, 3, 6], target_col="pm25_clean")

    # Row 0 (T=0, val=10) should have target_T+1 = 20, target_T+3 = 40, target_T+6 = 70
    assert targeted["target_pm25_t_plus_1"].iloc[0] == 20.0
    assert targeted["target_pm25_t_plus_3"].iloc[0] == 40.0
    assert targeted["target_pm25_t_plus_6"].iloc[0] == 70.0

    # End rows where future is unavailable must be NaN (no future wrapping)
    assert pd.isna(targeted["target_pm25_t_plus_1"].iloc[-1])
    assert pd.isna(targeted["target_pm25_t_plus_6"].iloc[-6])


def test_chronological_split_strict_monotonicity():
    dates = pd.date_range(start="2026-03-01 00:00:00", periods=30, freq="h", tz="UTC")
    df = pd.DataFrame({
        "hourly_bin": dates,
        "station_id": ["ST_01"] * 30,
        "pm25_clean": np.linspace(20, 80, 30)
    })
    train, val, test = split_chronologically(df, train_ratio=0.70, val_ratio=0.15)

    assert len(train) > 0
    assert len(val) > 0
    assert len(test) > 0

    # Strict chronological boundary check: train < val < test
    assert train["hourly_bin"].max() < val["hourly_bin"].min()
    assert val["hourly_bin"].max() < test["hourly_bin"].min()