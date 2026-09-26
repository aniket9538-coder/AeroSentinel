"""
AeroSentinel - NASA FIRMS Active Fire Feature Extraction Module
File: ai-service/preprocessing/fire.py

Processes thermal anomalies, applies confidence filtering, calculates inverse-distance
weighted Fire Radiative Power (FRP), and evaluates wind-aligned upwind fire advection.
"""

from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple
import math
import numpy as np
import pandas as pd
from app.schemas.canonical import QualityFlag


def clean_fire_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    Cleans raw NASA FIRMS detections, normalizes timestamps to UTC,
    validates coordinate bounds, filters to the regional monitoring domain,
    and maps confidence scores.
    Handles NASA FIRMS separate acq_date and acq_time columns.
    """
    if df.empty:
        return pd.DataFrame()

    clean_df = df.copy()
    clean_df.columns = clean_df.columns.str.strip().str.lower()

    # 1. Resolve and combine NASA FIRMS date and time columns
    if "detected_at" not in clean_df.columns:
        if "acq_date" in clean_df.columns and "acq_time" in clean_df.columns:
            # Ensure acq_time is a 4-digit string with leading zeros (e.g., 345 -> '0345')
            acq_time_clean = clean_df["acq_time"].astype(str).str.split(".").str[0].str.zfill(4)
            clean_df["detected_at"] = pd.to_datetime(
                clean_df["acq_date"].astype(str) + " " + acq_time_clean,
                format="%Y-%m-%d %H%M",
                utc=True,
                errors="coerce"
            )
        else:
            ts_col = next((c for c in ["timestamp", "acq_datetime", "datetime", "observed_at"] if c in clean_df.columns), None)
            if ts_col:
                clean_df["detected_at"] = pd.to_datetime(clean_df[ts_col], utc=True, errors="coerce")
            else:
                raise ValueError("Missing detection timestamp column in fire data.")
    else:
        clean_df["detected_at"] = pd.to_datetime(clean_df["detected_at"], utc=True, errors="coerce")

    clean_df = clean_df.dropna(subset=["detected_at"]).copy()

    # 2. Regional coordinate boundaries check (Pune Metropolitan Region & surrounding buffer)
    # Filters out all irrelevant fires across the rest of the country (reduces ~1.9M to ~2K records)
    clean_df["latitude"] = pd.to_numeric(clean_df["latitude"], errors="coerce")
    clean_df["longitude"] = pd.to_numeric(clean_df["longitude"], errors="coerce")
    regional_mask = (
        clean_df["latitude"].between(17.5, 19.5) &
        clean_df["longitude"].between(72.8, 75.0)
    )
    clean_df = clean_df[regional_mask].copy()

    # 3. FRP and confidence normalization
    if "frp" in clean_df.columns:
        clean_df["frp"] = pd.to_numeric(clean_df["frp"], errors="coerce").fillna(0.0).clip(lower=0.0)
    else:
        clean_df["frp"] = 0.0

    if "confidence" in clean_df.columns:
        def _parse_conf(val):
            if isinstance(val, str):
                v = val.strip().lower()
                if v in ["l", "low"]: return 30.0
                if v in ["n", "nominal"]: return 65.0
                if v in ["h", "high"]: return 90.0
            try:
                return float(val)
            except (ValueError, TypeError):
                return 50.0
        clean_df["confidence_num"] = clean_df["confidence"].apply(_parse_conf)
    else:
        clean_df["confidence_num"] = 50.0

    return clean_df.sort_values(by="detected_at").reset_index(drop=True)


def haversine_distance_km_vectorized(
    lat1: float, lon1: float,
    lats: np.ndarray, lons: np.ndarray
) -> np.ndarray:
    """Computes Haversine distance in km between a point and an array of points."""
    R = 6371.0
    phi1 = np.radians(lat1)
    phi2 = np.radians(lats)
    dphi = np.radians(lats - lat1)
    dlambda = np.radians(lons - lon1)

    a = np.sin(dphi / 2.0) ** 2 + np.cos(phi1) * np.cos(phi2) * np.sin(dlambda / 2.0) ** 2
    return 2.0 * R * np.arctan2(np.sqrt(a), np.sqrt(1.0 - a))


def calculate_bearing_degrees(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculates forward great-circle bearing from point 1 to point 2 in degrees (0=North, 90=East)."""
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dlam = math.radians(lon2 - lon1)
    y = math.sin(dlam) * math.cos(phi2)
    x = math.cos(phi1) * math.sin(phi2) - math.sin(phi1) * math.cos(phi2) * math.cos(dlam)
    return (math.degrees(math.atan2(y, x)) + 360.0) % 360.0


def extract_fire_features_for_observation(
    obs_lat: float,
    obs_lon: float,
    obs_time: datetime,
    wind_speed: float,
    wind_direction: float,
    fires_df: pd.DataFrame,
    lookback_hours: int = 24,
    radius_km: float = 25.0
) -> Dict[str, float]:
    """
    Computes fire features strictly for fires occurring within [obs_time - lookback_hours, obs_time].
    No future fires are ever evaluated (strict temporal leakage guard).
    """
    default_features = {
        "fire_count_24h_25km": 0,
        "fire_frp_sum_24h_25km": 0.0,
        "fire_frp_mean_24h_25km": 0.0,
        "nearest_fire_distance_km": 50.0,
        "fire_frp_distance_decay": 0.0,
        "fire_upwind_alignment_score": 0.0,
    }

    if fires_df.empty:
        return default_features

    # Strict lookback window: strictly past or current detections
    start_window = obs_time - timedelta(hours=lookback_hours)
    mask = (fires_df["detected_at"] >= start_window) & (fires_df["detected_at"] <= obs_time)
    past_fires = fires_df[mask]

    if past_fires.empty:
        return default_features

    distances = haversine_distance_km_vectorized(
        obs_lat, obs_lon,
        past_fires["latitude"].values,
        past_fires["longitude"].values
    )

    within_radius_idx = np.where(distances <= radius_km)[0]
    min_dist = float(np.min(distances)) if len(distances) > 0 else 50.0

    if len(within_radius_idx) == 0:
        default_features["nearest_fire_distance_km"] = round(min(min_dist, 50.0), 2)
        return default_features

    close_fires = past_fires.iloc[within_radius_idx]
    close_dists = distances[within_radius_idx]

    frp_vals = close_fires["frp"].values
    frp_sum = float(np.sum(frp_vals))
    frp_mean = float(np.mean(frp_vals))

    # Inverse-distance weighted FRP: sum(FRP / (dist_km + 1.0))
    frp_decay = float(np.sum(frp_vals / (close_dists + 1.0)))

    # Upwind Alignment: Wind direction is where wind comes FROM.
    # If bearing from station to fire matches wind direction, fire is UPWIND of the station.
    upwind_score = 0.0
    if wind_speed >= 0.2 and not np.isnan(wind_direction):
        for idx, row in close_fires.iterrows():
            bearing = calculate_bearing_degrees(obs_lat, obs_lon, row["latitude"], row["longitude"])
            # Angular difference on circle
            diff = abs((bearing - wind_direction + 180.0) % 360.0 - 180.0)
            if diff <= 45.0:  # Fire is within +/- 45 deg of upwind corridor
                weight = math.cos(math.radians(diff))
                dist = haversine_distance_km_vectorized(
                    obs_lat, obs_lon,
                    np.array([row["latitude"]]),
                    np.array([row["longitude"]])
                )[0]
                upwind_score += float(row["frp"] * weight / (dist + 1.0))

    return {
        "fire_count_24h_25km": int(len(within_radius_idx)),
        "fire_frp_sum_24h_25km": round(frp_sum, 2),
        "fire_frp_mean_24h_25km": round(frp_mean, 2),
        "nearest_fire_distance_km": round(min_dist, 2),
        "fire_frp_distance_decay": round(frp_decay, 4),
        "fire_upwind_alignment_score": round(upwind_score, 4),
    }