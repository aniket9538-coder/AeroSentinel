"""
AeroSentinel - Production F3 Feature Data Layer Service (Python)
File: ai-service/ml/features/feature_service.py

Authoritative 36-feature vector generation matching:
  - hotspot_classifier_v1.joblib
  - train_all_models.py select_features()
  - com.aerosentinel.feature.FeatureRecord (Java Backend)
"""

from datetime import datetime, timezone
import math
from typing import Dict, List, Optional, Tuple, Any
import numpy as np
import pandas as pd


FEATURE_SCHEMA_VERSION = "f3-features-v1"
FEATURE_COUNT = 36

ORDERED_FEATURE_NAMES = [
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
    "fire_upwind_alignment_score"
]

# Baseline defaults for Pune Metropolitan Region per Member 3 preprocessing contract
PUNE_BASELINE_GIS = {
    "dist_to_nearest_industrial_km": 3.50,
    "dist_to_nearest_major_road_km": 0.40,
    "sensitive_receptors_count_2km": 4,
    "industrial_zone_within_2km_flag": 0
}
DEFAULT_PUNE_PRESSURE = 954.3  # hPa (elevation ~600m)


def haversine_distance_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Great-circle distance between two WGS84 points in kilometers."""
    r = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlam = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2.0) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlam / 2.0) ** 2
    return 2.0 * r * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))


def calculate_bearing_degrees(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculates forward great-circle bearing from point 1 to point 2 in degrees (0=North, 90=East)."""
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dlam = math.radians(lon2 - lon1)
    y = math.sin(dlam) * math.cos(phi2)
    x = math.cos(phi1) * math.sin(phi2) - math.sin(phi1) * math.cos(phi2) * math.cos(dlam)
    return (math.degrees(math.atan2(y, x)) + 360.0) % 360.0


def derive_wind_vectors(wind_speed_kmh: float, wind_dir_deg: float) -> Tuple[float, float]:
    """
    Exact meteorological wind velocity derivation:
      speed in m/s: ws = wind_speed_kmh / 3.6
      calm threshold: ws < 0.2 m/s -> u = 0.0, v = 0.0
      direction is FROM which wind blows: u = -ws * sin(rad), v = -ws * cos(rad)
    """
    if wind_speed_kmh is None or wind_dir_deg is None:
        return 0.0, 0.0
    speed_ms = float(wind_speed_kmh) / 3.6
    if speed_ms < 0.2:
        return 0.0, 0.0
    rad = math.radians(wind_dir_deg)
    u = -speed_ms * math.sin(rad)
    v = -speed_ms * math.cos(rad)
    return round(u, 4), round(v, 4)


def compute_leave_one_out_spatial_lag(station_pm25_values: List[float], self_index: int) -> float:
    """
    Strict leave-one-out spatial lag for PM2.5 to eliminate self-information leakage:
      lag = (sum(all_stations) - self_value) / (N - 1)
    """
    if not station_pm25_values:
        return 0.0
    if len(station_pm25_values) == 1:
        return float(station_pm25_values[0])
    
    other_vals = [v for i, v in enumerate(station_pm25_values) if i != self_index]
    return round(float(np.mean(other_vals)), 2)


def compute_temporal_features(utc_dt: datetime) -> Dict[str, Any]:
    """
    Computes cyclical diurnal and day-of-week temporal features anchored to Asia/Kolkata local solar time.
    """
    # Convert to Asia/Kolkata (+05:30)
    ts = pd.to_datetime(utc_dt, utc=True).tz_convert("Asia/Kolkata")
    hour = int(ts.hour)
    dow = int(ts.dayofweek)  # 0=Monday .. 6=Sunday
    is_weekend = 1 if dow >= 5 else 0

    hour_sin = round(float(np.sin(2.0 * np.pi * hour / 24.0)), 4)
    hour_cos = round(float(np.cos(2.0 * np.pi * hour / 24.0)), 4)
    dow_sin = round(float(np.sin(2.0 * np.pi * dow / 7.0)), 4)
    dow_cos = round(float(np.cos(2.0 * np.pi * dow / 7.0)), 4)

    return {
        "hour": hour,
        "day_of_week": dow,
        "is_weekend": is_weekend,
        "hour_sin": hour_sin,
        "hour_cos": hour_cos,
        "dow_sin": dow_sin,
        "dow_cos": dow_cos
    }


def compute_monitoring_coverage(
    obs_lat: float,
    obs_lon: float,
    station_coords: List[Tuple[float, float]]
) -> Dict[str, Any]:
    """
    Computes monitoring network density and coverage gap flag (> 7.0 km).
    """
    if not station_coords:
        return {
            "nearest_station_distance_km": 0.0,
            "stations_within_5km_count": 1,
            "monitoring_coverage_gap_flag": 0
        }
    
    dists = []
    c_5km = 1  # includes self
    for lat, lon in station_coords:
        d = haversine_distance_km(obs_lat, obs_lon, lat, lon)
        if d < 1e-4:
            continue
        dists.append(d)
        if d <= 5.0:
            c_5km += 1

    min_dist = min(dists) if dists else 0.0
    gap_flag = 1 if min_dist > 7.0 else 0

    return {
        "nearest_station_distance_km": round(min_dist, 2),
        "stations_within_5km_count": c_5km,
        "monitoring_coverage_gap_flag": gap_flag
    }


def compute_fire_features(
    obs_lat: float,
    obs_lon: float,
    wind_speed_ms: float,
    wind_dir_deg: float,
    fires: List[Dict[str, Any]],
    obs_time: Optional[datetime] = None
) -> Dict[str, Any]:
    """
    NASA FIRMS active fire features with strict 24h lookback and 25km radius.
    Guarantees no future fire leakage: fire_time <= obs_time.
    Legitimate domain-defined zero fill on absence of fires.
    """
    defaults = {
        "fire_count_24h_25km": 0,
        "fire_frp_sum_24h_25km": 0.0,
        "fire_frp_mean_24h_25km": 0.0,
        "nearest_fire_distance_km": 50.0,
        "fire_frp_distance_decay": 0.0,
        "fire_upwind_alignment_score": 0.0
    }
    if not fires:
        return defaults

    # Strict temporal filter: [obs_time - 24h, obs_time]
    valid_fires = []
    for f in fires:
        f_time = pd.to_datetime(f.get("detected_at"), utc=True).to_pydatetime()
        if obs_time is not None:
            if f_time > obs_time or (obs_time - f_time).total_seconds() > 86400:
                continue
        valid_fires.append(f)

    if not valid_fires:
        return defaults

    min_dist = 50.0
    frp_vals = []
    close_fires = []

    for f in valid_fires:
        d = haversine_distance_km(obs_lat, obs_lon, f["latitude"], f["longitude"])
        if d < min_dist:
            min_dist = d
        if d <= 25.0:
            close_fires.append((f, d))
            frp_vals.append(float(f.get("frp", 0.0)))

    if not close_fires:
        defaults["nearest_fire_distance_km"] = round(min(min_dist, 50.0), 2)
        return defaults

    frp_sum = sum(frp_vals)
    frp_mean = frp_sum / len(frp_vals)
    frp_decay = sum(f.get("frp", 0.0) / (d + 1.0) for f, d in close_fires)

    upwind_score = 0.0
    if wind_speed_ms >= 0.2 and wind_dir_deg is not None:
        for f, d in close_fires:
            bearing = calculate_bearing_degrees(obs_lat, obs_lon, f["latitude"], f["longitude"])
            diff = abs((bearing - wind_dir_deg + 180.0) % 360.0 - 180.0)
            if diff <= 45.0:
                weight = math.cos(math.radians(diff))
                upwind_score += float(f.get("frp", 0.0)) * weight / (d + 1.0)

    return {
        "fire_count_24h_25km": len(close_fires),
        "fire_frp_sum_24h_25km": round(frp_sum, 2),
        "fire_frp_mean_24h_25km": round(frp_mean, 2),
        "nearest_fire_distance_km": round(min_dist, 2),
        "fire_frp_distance_decay": round(frp_decay, 4),
        "fire_upwind_alignment_score": round(upwind_score, 4)
    }


def assemble_feature_vector(
    raw_inputs: Dict[str, Any],
    station_coords: Optional[List[Tuple[float, float]]] = None,
    concurrent_pm25: Optional[List[float]] = None,
    self_pm25_idx: int = 0,
    fires: Optional[List[Dict[str, Any]]] = None
) -> Tuple[Dict[str, float], List[str], str]:
    """
    Assembles the exact 36-feature vector matching the trained Random Forest contract.
    Returns:
      (ordered_feature_dict, missing_fields_list, quality_status)
    """
    missing_fields = []
    
    lat = float(raw_inputs.get("latitude", 18.5204))
    lon = float(raw_inputs.get("longitude", 73.8567))

    # Co-pollutants
    for pol in ["pm10", "no2", "so2", "co", "o3"]:
        if raw_inputs.get(pol) is None:
            missing_fields.append(pol)

    pm10 = float(raw_inputs.get("pm10", 0.0) or 0.0)
    no2 = float(raw_inputs.get("no2", 0.0) or 0.0)
    so2 = float(raw_inputs.get("so2", 0.0) or 0.0)
    co = float(raw_inputs.get("co", 0.0) or 0.0)
    o3 = float(raw_inputs.get("o3", 0.0) or 0.0)

    # Weather
    temp = float(raw_inputs.get("temperature", 25.0) or 25.0)
    humidity = float(raw_inputs.get("humidity", 50.0) or 50.0)
    ws_kmh = float(raw_inputs.get("wind_speed", 5.0) or 5.0)
    wind_dir = float(raw_inputs.get("wind_direction", 0.0) or 0.0)
    rainfall = max(0.0, float(raw_inputs.get("rainfall", 0.0) or 0.0))
    pressure = float(raw_inputs.get("pressure", DEFAULT_PUNE_PRESSURE) or DEFAULT_PUNE_PRESSURE)

    # Wind decomposition
    wind_u, wind_v = derive_wind_vectors(ws_kmh, wind_dir)
    ws_ms = ws_kmh / 3.6

    # Spatial lag
    if concurrent_pm25 is not None and len(concurrent_pm25) > 0:
        spatial_lag = compute_leave_one_out_spatial_lag(concurrent_pm25, self_pm25_idx)
    else:
        spatial_lag = float(raw_inputs.get("pm25_spatial_lag_mean", raw_inputs.get("pm25", 50.0)))

    # Temporal
    obs_time = raw_inputs.get("observed_at")
    if isinstance(obs_time, str):
        obs_time = pd.to_datetime(obs_time, utc=True).to_pydatetime()
    elif obs_time is None:
        obs_time = datetime.now(timezone.utc)
    temp_feats = compute_temporal_features(obs_time)

    # Monitoring coverage
    mon_feats = compute_monitoring_coverage(lat, lon, station_coords or [])

    # Active fires
    fire_feats = compute_fire_features(lat, lon, ws_ms, wind_dir, fires or [], obs_time=obs_time)

    # GIS
    gis_feats = {
        "dist_to_nearest_industrial_km": float(raw_inputs.get("dist_to_nearest_industrial_km", PUNE_BASELINE_GIS["dist_to_nearest_industrial_km"])),
        "dist_to_nearest_major_road_km": float(raw_inputs.get("dist_to_nearest_major_road_km", PUNE_BASELINE_GIS["dist_to_nearest_major_road_km"])),
        "sensitive_receptors_count_2km": int(raw_inputs.get("sensitive_receptors_count_2km", PUNE_BASELINE_GIS["sensitive_receptors_count_2km"])),
        "industrial_zone_within_2km_flag": int(raw_inputs.get("industrial_zone_within_2km_flag", PUNE_BASELINE_GIS["industrial_zone_within_2km_flag"]))
    }

    feature_dict = {
        "latitude": round(lat, 4),
        "longitude": round(lon, 4),
        "pm10": round(pm10, 2),
        "no2": round(no2, 2),
        "so2": round(so2, 2),
        "co": round(co, 2),
        "o3": round(o3, 2),
        "hour": temp_feats["hour"],
        "day_of_week": temp_feats["day_of_week"],
        "is_weekend": temp_feats["is_weekend"],
        "hour_sin": temp_feats["hour_sin"],
        "hour_cos": temp_feats["hour_cos"],
        "dow_sin": temp_feats["dow_sin"],
        "dow_cos": temp_feats["dow_cos"],
        "temperature": round(temp, 2),
        "humidity": round(humidity, 2),
        "wind_speed": round(ws_kmh, 2),
        "wind_direction": round(wind_dir, 2),
        "wind_u": wind_u,
        "wind_v": wind_v,
        "rainfall": round(rainfall, 2),
        "pressure": round(pressure, 2),
        "pm25_spatial_lag_mean": round(spatial_lag, 2),
        "nearest_station_distance_km": mon_feats["nearest_station_distance_km"],
        "stations_within_5km_count": mon_feats["stations_within_5km_count"],
        "monitoring_coverage_gap_flag": mon_feats["monitoring_coverage_gap_flag"],
        "dist_to_nearest_industrial_km": gis_feats["dist_to_nearest_industrial_km"],
        "dist_to_nearest_major_road_km": gis_feats["dist_to_nearest_major_road_km"],
        "sensitive_receptors_count_2km": gis_feats["sensitive_receptors_count_2km"],
        "industrial_zone_within_2km_flag": gis_feats["industrial_zone_within_2km_flag"],
        "fire_count_24h_25km": fire_feats["fire_count_24h_25km"],
        "fire_frp_sum_24h_25km": fire_feats["fire_frp_sum_24h_25km"],
        "fire_frp_mean_24h_25km": fire_feats["fire_frp_mean_24h_25km"],
        "nearest_fire_distance_km": fire_feats["nearest_fire_distance_km"],
        "fire_frp_distance_decay": fire_feats["fire_frp_distance_decay"],
        "fire_upwind_alignment_score": fire_feats["fire_upwind_alignment_score"]
    }

    quality_status = "VALID" if not missing_fields else ("UNAVAILABLE" if len(missing_fields) > 3 else "MISSING")
    return feature_dict, missing_fields, quality_status
