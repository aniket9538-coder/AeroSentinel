"""
AeroSentinel - Weather Cleaning & Preprocessing Pipeline
File: ai-service/preprocessing/weather.py

Surface meteorology cleaning, boundary condition validations, calm-wind resolution,
and orthogonal wind velocity vector derivation (U and V).
"""

from typing import Dict, Tuple
import numpy as np
import pandas as pd
from app.schemas.canonical import QualityFlag


def clean_weather_dataframe(df: pd.DataFrame) -> Tuple[pd.DataFrame, Dict[str, int]]:
    """
    Cleans raw weather data according to F1-B requirements.
    
    Returns:
        Tuple of (cleaned_df, audit_stats_dict)
    """
    if df.empty:
        return pd.DataFrame(), {"raw_rows": 0, "cleaned_rows": 0}

    stats = {
        "raw_rows": len(df),
        "duplicates_removed": 0,
        "calm_winds_resolved": 0,
        "out_of_bounds_flagged": 0,
        "cleaned_rows": 0,
    }

    clean_df = df.copy()
    clean_df.columns = clean_df.columns.str.strip().str.lower()

    # Resolve timestamp column
    ts_col = next((c for c in ["observed_at", "timestamp", "datetime", "date"] if c in clean_df.columns), None)
    if not ts_col:
        raise ValueError("Missing timestamp column in weather input.")

    clean_df["observed_at"] = pd.to_datetime(clean_df[ts_col], utc=True, errors="coerce")
    clean_df = clean_df.dropna(subset=["observed_at"]).copy()
    clean_df["hourly_bin"] = clean_df["observed_at"].dt.floor("h")

    # City identifier or spatial coordinate anchoring
    if "city_id" not in clean_df.columns:
        clean_df["city_id"] = "city-pune"

    # Deduplicate by (city_id, hourly_bin)
    initial_len = len(clean_df)
    clean_df = clean_df.sort_values(by=["city_id", "hourly_bin"]).drop_duplicates(
        subset=["city_id", "hourly_bin"], keep="last"
    )
    stats["duplicates_removed"] = initial_len - len(clean_df)

    # Numeric conversions
    for col in ["temperature", "humidity", "wind_speed", "wind_direction", "rainfall", "pressure"]:
        if col in clean_df.columns:
            clean_df[col] = pd.to_numeric(clean_df[col], errors="coerce")

    # Range assertions
    if "humidity" in clean_df.columns:
        invalid_hum = (clean_df["humidity"] < 0.0) | (clean_df["humidity"] > 100.0)
        if invalid_hum.any():
            stats["out_of_bounds_flagged"] += int(invalid_hum.sum())
            clean_df.loc[invalid_hum, "humidity"] = np.nan

    if "temperature" in clean_df.columns:
        invalid_temp = (clean_df["temperature"] < -20.0) | (clean_df["temperature"] > 60.0)
        if invalid_temp.any():
            stats["out_of_bounds_flagged"] += int(invalid_temp.sum())
            clean_df.loc[invalid_temp, "temperature"] = np.nan

    if "rainfall" in clean_df.columns:
        clean_df["rainfall"] = clean_df["rainfall"].clip(lower=0.0).fillna(0.0)

    # Wind vectorization and calm resolution
    if "wind_speed" in clean_df.columns and "wind_direction" in clean_df.columns:
        speed = clean_df["wind_speed"].fillna(0.0)
        direction = clean_df["wind_direction"].fillna(0.0)

        # Calm conditions: speed < 0.2 m/s
        is_calm = speed < 0.2
        stats["calm_winds_resolved"] = int(is_calm.sum())

        rad = np.radians(direction)
        # Vector points toward where the wind blows: U = -ws*sin(rad), V = -ws*cos(rad)
        u = -speed * np.sin(rad)
        v = -speed * np.cos(rad)

        u = np.where(is_calm, 0.0, u)
        v = np.where(is_calm, 0.0, v)

        clean_df["wind_u"] = np.round(u, 4)
        clean_df["wind_v"] = np.round(v, 4)
    else:
        clean_df["wind_u"] = 0.0
        clean_df["wind_v"] = 0.0

    clean_df["weather_quality_flag"] = QualityFlag.VALID.value
    stats["cleaned_rows"] = len(clean_df)
    return clean_df, stats