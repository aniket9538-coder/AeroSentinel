"""
AeroSentinel - Spatial Feature Engineering Module
File: ai-service/preprocessing/spatial_features.py

Implements spatial lag computations, vector buffer extractions,
monitoring station coverage gaps, and GIS proximity measures.
"""

from pathlib import Path
from typing import Dict, List, Optional, Tuple
import json
import math
import numpy as np
import pandas as pd
import shapely.geometry
from shapely.ops import nearest_points
import h3


def get_h3_k_ring_neighbors(cell_id: str, k: int = 1) -> List[str]:
    """
    Returns the k-ring neighbor H3 cells compatible across H3 v3 and v4.
    """
    if not cell_id:
        return []
    clean_cell = cell_id.strip().lower()
    if hasattr(h3, "grid_disk"):
        return list(h3.grid_disk(clean_cell, k))  # H3 v4 API
    elif hasattr(h3, "k_ring"):
        return list(h3.k_ring(clean_cell, k))      # H3 v3 API
    return []


def haversine_distance_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    Great-circle distance between two WGS84 points in kilometers.
    """
    r = 6371.0  # Earth radius in km
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = (
        math.sin(delta_phi / 2.0) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2.0) ** 2
    )
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    return r * c


def compute_spatial_lag(
    df: pd.DataFrame,
    target_col: str = "pm25_clean",
    time_col: str = "hourly_bin",
    station_col: str = "station_id"
) -> pd.DataFrame:
    """
    Computes spatial lag feature (mean of neighboring stations at the SAME timestamp).
    Applies leave-one-out calculation per timestamp to prevent target leakage:
      spatial_lag = (sum(all_stations) - self_station) / (n_stations - 1)
    """
    if df.empty or target_col not in df.columns:
        return df

    out_df = df.copy()

    # Precompute city-wide leave-one-out mean per timestamp
    time_grouped = out_df.groupby(time_col)[target_col]
    time_sum = time_grouped.transform("sum")
    time_count = time_grouped.transform("count")

    # Leave-one-out: (sum - self_value) / (count - 1)
    denom = (time_count - 1).replace(0, np.nan)
    spatial_lag = (time_sum - out_df[target_col]) / denom

    # Fallback to own value if only one station exists at that time bin
    out_df["pm25_spatial_lag_mean"] = np.round(
        spatial_lag.fillna(out_df[target_col]), 2
    )

    return out_df


# Alias for plural naming compatibility across pipelines
compute_spatial_lags = compute_spatial_lag


def extract_monitoring_coverage_features(
    df: pd.DataFrame,
    stations_geojson: Path
) -> pd.DataFrame:
    """
    Computes monitoring network coverage metrics:
      - nearest_station_distance_km: distance to the nearest distinct monitoring station
      - stations_within_5km_count: count of stations within 5 km radius
      - monitoring_coverage_gap_flag: 1 if nearest station > 7 km, representing higher uncertainty
    """
    if df.empty:
        return df

    out_df = df.copy()

    if not stations_geojson.exists():
        out_df["nearest_station_distance_km"] = 0.0
        out_df["stations_within_5km_count"] = 1
        out_df["monitoring_coverage_gap_flag"] = 0
        return out_df

    try:
        with open(stations_geojson, "r", encoding="utf-8") as f:
            geo = json.load(f)

        coords = [feat["geometry"]["coordinates"] for feat in geo.get("features", [])]
        station_pts = [shapely.geometry.Point(c[0], c[1]) for c in coords if len(c) >= 2]

        if not station_pts:
            out_df["nearest_station_distance_km"] = 0.0
            out_df["stations_within_5km_count"] = 1
            out_df["monitoring_coverage_gap_flag"] = 0
            return out_df

        locs = out_df[["station_id", "latitude", "longitude"]].drop_duplicates()
        dist_map = {}
        count_map = {}

        for _, row in locs.iterrows():
            pt = shapely.geometry.Point(row["longitude"], row["latitude"])
            dists = [
                haversine_distance_km(pt.y, pt.x, st.y, st.x)
                for st in station_pts
                if not (abs(st.x - pt.x) < 1e-5 and abs(st.y - pt.y) < 1e-5)
            ]
            min_d = min(dists) if dists else 0.0
            c_5km = sum(1 for d in dists if d <= 5.0) + 1  # include self station

            dist_map[row["station_id"]] = round(min_d, 2)
            count_map[row["station_id"]] = int(c_5km)

        out_df["nearest_station_distance_km"] = out_df["station_id"].map(dist_map).fillna(0.0)
        out_df["stations_within_5km_count"] = out_df["station_id"].map(count_map).fillna(1).astype(int)
        out_df["monitoring_coverage_gap_flag"] = (out_df["nearest_station_distance_km"] > 7.0).astype(int)
    except Exception:
        out_df["nearest_station_distance_km"] = 0.0
        out_df["stations_within_5km_count"] = 1
        out_df["monitoring_coverage_gap_flag"] = 0

    return out_df


def extract_gis_proximity_features(
    df: pd.DataFrame,
    geo_dir: Path
) -> pd.DataFrame:
    """
    Computes distance in km from each station to key urban infrastructure:
      - dist_to_nearest_industrial_km
      - industrial_zone_within_2km_flag
      - dist_to_nearest_major_road_km
      - sensitive_receptors_count_2km
    """
    if df.empty or "latitude" not in df.columns or "longitude" not in df.columns:
        return df

    out_df = df.copy()
    locs = out_df[["station_id", "latitude", "longitude"]].drop_duplicates()

    # Regional default baselines for Pune Metropolitan Region if GeoJSONs are absent
    ind_map = {sid: 3.50 for sid in locs["station_id"]}
    road_map = {sid: 0.40 for sid in locs["station_id"]}
    receptors_map = {sid: 4 for sid in locs["station_id"]}

    # 1. Industrial Zones Proximity
    ind_file = geo_dir / "industrial_zones.geojson"
    if ind_file.exists():
        try:
            with open(ind_file, "r", encoding="utf-8") as f:
                ind_geo = json.load(f)
            polygons = [shapely.geometry.shape(feat["geometry"]) for feat in ind_geo.get("features", [])]
            if polygons:
                multi_poly = (
                    shapely.geometry.MultiPolygon([p for p in polygons if p.geom_type == "Polygon"])
                    if all(p.geom_type == "Polygon" for p in polygons)
                    else polygons[0]
                )
                for _, row in locs.iterrows():
                    pt = shapely.geometry.Point(row["longitude"], row["latitude"])
                    deg_dist = pt.distance(multi_poly)
                    ind_map[row["station_id"]] = round(deg_dist * 111.0, 2)
        except Exception:
            pass

    # 2. Road Network Proximity
    roads_file = geo_dir / "roads.geojson"
    if roads_file.exists():
        try:
            with open(roads_file, "r", encoding="utf-8") as f:
                roads_geo = json.load(f)
            lines = [shapely.geometry.shape(feat["geometry"]) for feat in roads_geo.get("features", [])]
            if lines:
                multi_line = shapely.geometry.MultiLineString([l for l in lines if l.geom_type == "LineString"])
                for _, row in locs.iterrows():
                    pt = shapely.geometry.Point(row["longitude"], row["latitude"])
                    deg_dist = pt.distance(multi_line)
                    road_map[row["station_id"]] = round(deg_dist * 111.0, 3)
        except Exception:
            pass

    # 3. Sensitive Receptors Count (Schools + Hospitals within 2km)
    schools_file = geo_dir / "schools.geojson"
    hospitals_file = geo_dir / "hospitals.geojson"
    receptor_pts = []
    for fp in [schools_file, hospitals_file]:
        if fp.exists():
            try:
                with open(fp, "r", encoding="utf-8") as f:
                    geo_rec = json.load(f)
                receptor_pts.extend([shapely.geometry.shape(feat["geometry"]) for feat in geo_rec.get("features", [])])
            except Exception:
                pass

    if receptor_pts:
        for _, row in locs.iterrows():
            pt = shapely.geometry.Point(row["longitude"], row["latitude"])
            receptors_map[row["station_id"]] = sum(1 for rec in receptor_pts if (pt.distance(rec) * 111.0) <= 2.0)

    out_df["dist_to_nearest_industrial_km"] = out_df["station_id"].map(ind_map)
    out_df["dist_to_nearest_major_road_km"] = out_df["station_id"].map(road_map)
    out_df["sensitive_receptors_count_2km"] = out_df["station_id"].map(receptors_map)
    out_df["industrial_zone_within_2km_flag"] = (out_df["dist_to_nearest_industrial_km"] <= 2.0).astype(int)

    return out_df