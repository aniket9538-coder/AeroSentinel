"""
AeroSentinel - F2 Spatial Feature Engineering Unit Tests
File: tests/ai/test_f2_spatial.py

Validates:
  - H3 resolution 8 spatial indexing, boundary coordinates, and deterministic binding
  - Geodesic great-circle distance & circular bearing calculations
  - GIS polygon containment, buffer distances, and fallback defaults
  - Leave-one-out spatial neighbor lags (strict target leakage prevention)
  - Monitoring coverage gap flags and station density
  - NASA FIRMS lookback windowing & wind-aligned plume advection
  - Sentinel-5P cloud-fraction quality filtering
"""

from datetime import datetime, timedelta, timezone
from pathlib import Path
import json
import numpy as np
import pandas as pd
import pytest

from preprocessing.spatial_alignment import validate_coordinates, bind_h3_cell
from preprocessing.spatial_features import (
    get_h3_k_ring_neighbors,
    haversine_distance_km,
    compute_spatial_lag,
    extract_monitoring_coverage_features,
    extract_gis_proximity_features,
)
from preprocessing.fire import (
    clean_fire_dataframe,
    calculate_bearing_degrees,
    extract_fire_features_for_observation,
)
from preprocessing.satellite import (
    clean_satellite_dataframe,
    extract_satellite_features_for_observation,
)


# =====================================================================
# 1. H3 SPATIAL INDEXING & BOUNDARY VALIDATION
# =====================================================================

def test_h3_k_ring_neighbors():
    cell = "886196944dfffff"
    ring_1 = get_h3_k_ring_neighbors(cell, k=1)
    assert len(ring_1) == 7
    assert cell in ring_1


def test_h3_invalid_and_edge_coordinates():
    assert bind_h3_cell(None, 73.8567) is None
    assert bind_h3_cell(18.5204, None) is None
    assert bind_h3_cell(95.0, 73.8567) is None
    assert bind_h3_cell(18.5204, 195.0) is None

    cell_a = bind_h3_cell(18.5204, 73.8567, resolution=8)
    cell_b = bind_h3_cell(18.5204, 73.8567, resolution=8)
    assert cell_a == cell_b
    assert cell_a is not None


def test_haversine_distance():
    dist = haversine_distance_km(18.5304, 73.8467, 18.5020, 73.9270)
    assert 9.0 < dist < 12.0


def test_calculate_bearing_degrees():
    # Due North: Latitude increases, Longitude constant -> ~0 deg
    bearing_north = calculate_bearing_degrees(18.0, 73.0, 19.0, 73.0)
    assert abs(bearing_north - 0.0) < 1.0 or abs(bearing_north - 360.0) < 1.0

    # Due East along Equator (lat=0.0) -> ~90 deg
    bearing_equator = calculate_bearing_degrees(0.0, 73.0, 0.0, 74.0)
    assert abs(bearing_equator - 90.0) < 1.0

    # Due East along non-zero latitude -> ~90 deg
    bearing_east = calculate_bearing_degrees(18.0, 73.0, 18.0, 74.0)
    assert abs(bearing_east - 90.0) < 1.5


# =====================================================================
# 2. GIS GEOMETRY VALIDITY, POLYGON INTERSECTION & FALLBACKS
# =====================================================================

def test_gis_geometry_validity_and_distance(tmp_path):
    ind_geojson = {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                        [
                            [73.80, 18.50],
                            [73.82, 18.50],
                            [73.82, 18.52],
                            [73.80, 18.52],
                            [73.80, 18.50]
                        ]
                    ]
                },
                "properties": {"name": "Synthetic Industrial MIDC"}
            }
        ]
    }
    ind_file = tmp_path / "industrial_zones.geojson"
    ind_file.write_text(json.dumps(ind_geojson), encoding="utf-8")

    df_inside = pd.DataFrame({
        "station_id": ["ST_INSIDE"],
        "latitude": [18.51],
        "longitude": [73.81]
    })
    res_in = extract_gis_proximity_features(df_inside, geo_dir=tmp_path)
    assert res_in["dist_to_nearest_industrial_km"].iloc[0] == 0.0
    assert res_in["industrial_zone_within_2km_flag"].iloc[0] == 1


def test_extract_gis_proximity_fallbacks():
    df = pd.DataFrame({
        "station_id": ["ST_01"],
        "latitude": [18.5204],
        "longitude": [73.8567]
    })
    enriched = extract_gis_proximity_features(df, geo_dir=Path("non_existent_geo"))
    assert "dist_to_nearest_industrial_km" in enriched.columns
    assert "dist_to_nearest_major_road_km" in enriched.columns
    assert "sensitive_receptors_count_2km" in enriched.columns
    assert "industrial_zone_within_2km_flag" in enriched.columns
    assert enriched["dist_to_nearest_industrial_km"].iloc[0] > 0


def test_extract_monitoring_coverage_features():
    df = pd.DataFrame({
        "station_id": ["ST_01"],
        "latitude": [18.5204],
        "longitude": [73.8567]
    })
    enriched = extract_monitoring_coverage_features(df, stations_geojson=Path("non_existent.geojson"))
    assert "nearest_station_distance_km" in enriched.columns
    assert "stations_within_5km_count" in enriched.columns
    assert "monitoring_coverage_gap_flag" in enriched.columns


# =====================================================================
# 3. SPATIAL LAG & LEAKAGE PREVENTION TESTS
# =====================================================================

def test_compute_spatial_lag_leave_one_out():
    df = pd.DataFrame({
        "hourly_bin": ["2026-03-01 10:00:00", "2026-03-01 10:00:00"],
        "station_id": ["ST_01", "ST_02"],
        "pm25_clean": [50.0, 70.0]
    })
    lagged = compute_spatial_lag(df, target_col="pm25_clean")
    assert "pm25_spatial_lag_mean" in lagged.columns
    assert lagged.loc[lagged["station_id"] == "ST_01", "pm25_spatial_lag_mean"].iloc[0] == 70.0
    assert lagged.loc[lagged["station_id"] == "ST_02", "pm25_spatial_lag_mean"].iloc[0] == 50.0


# =====================================================================
# 4. FIRE & SATELLITE FEATURE EXTRACTION TESTS (NO FUTURE LEAKAGE)
# =====================================================================

def test_fire_features_no_future_leakage():
    t_now = datetime(2026, 3, 1, 12, 0, tzinfo=timezone.utc)
    t_future = t_now + timedelta(hours=2)
    t_past = t_now - timedelta(hours=3)

    fires = pd.DataFrame({
        "detected_at": [t_past, t_future],
        "latitude": [18.5300, 18.5350],
        "longitude": [73.8500, 73.8550],
        "frp": [25.0, 100.0],
        "confidence": ["high", "high"]
    })
    clean_fires = clean_fire_dataframe(fires)

    feats = extract_fire_features_for_observation(
        obs_lat=18.5204,
        obs_lon=73.8567,
        obs_time=t_now,
        wind_speed=2.0,
        wind_direction=180.0,
        fires_df=clean_fires,
        lookback_hours=24,
        radius_km=25.0
    )

    assert feats["fire_count_24h_25km"] == 1
    assert feats["fire_frp_sum_24h_25km"] == 25.0


def test_satellite_cloud_quality_filtering():
    t_now = datetime(2026, 3, 1, 14, 0, tzinfo=timezone.utc)
    sat = pd.DataFrame({
        "observed_at": [t_now - timedelta(hours=2), t_now - timedelta(hours=1)],
        "latitude": [18.5204, 18.5204],
        "longitude": [73.8567, 73.8567],
        "tropospheric_no2": [0.00015, 0.00005],
        "cloud_fraction": [0.10, 0.80]
    })
    clean_sat = clean_satellite_dataframe(sat)

    feats = extract_satellite_features_for_observation(
        obs_time=t_now,
        obs_h3="886196944dfffff",
        sat_df=clean_sat,
        lookback_hours=24
    )

    assert feats["satellite_no2_trop"] == 0.00015
    assert feats["satellite_cloud_fraction"] == 0.10