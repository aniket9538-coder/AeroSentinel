"""
AeroSentinel - Physical Normalization & Quality Flagging
Implements unit checks, range validation, and wind vector transformations.
"""
from typing import Any, Dict, Optional, Tuple, Union
import numpy as np
import pandas as pd

from app.schemas.canonical import QualityFlag

# Physical limits defined in F0 Canonical Schema
POLLUTANT_RANGES: Dict[str, Dict[str, float]] = {
    "pm25": {"min": 0.0, "max": 1500.0, "extreme": 500.0},
    "pm2_5": {"min": 0.0, "max": 1500.0, "extreme": 500.0},
    "pm10": {"min": 0.0, "max": 2000.0, "extreme": 800.0},
    "no2":  {"min": 0.0, "max": 1000.0, "extreme": 300.0},
    "so2":  {"min": 0.0, "max": 1000.0, "extreme": 300.0},
    "co":   {"min": 0.0, "max": 100.0,  "extreme": 30.0},
    "o3":   {"min": 0.0, "max": 1000.0, "extreme": 300.0}
}

WEATHER_RANGES: Dict[str, Dict[str, float]] = {
    "temperature": {"min": -20.0, "max": 60.0},
    "humidity":    {"min": 0.0,   "max": 100.0},
    "wind_speed":  {"min": 0.0,   "max": 100.0},
    "wind_direction": {"min": 0.0, "max": 360.0},
    "rainfall":    {"min": 0.0,   "max": 500.0},
    "pressure":    {"min": 800.0, "max": 1100.0}
}


def classify_pollutant_value(value: Any, pollutant: str) -> Tuple[QualityFlag, Optional[float]]:
    """
    Validates pollutant concentration without silently clipping legitimate pollution spikes.
    Distinguishes sensor failures (< 0 or > physically impossible) from extreme events.
    """
    if pd.isna(value) or value is None:
        return QualityFlag.MISSING, None

    spec = POLLUTANT_RANGES.get(pollutant.lower())
    try:
        val = float(value)
    except (ValueError, TypeError):
        return QualityFlag.INVALID, None

    if not spec:
        return QualityFlag.VALID, val

    if val < spec["min"] or val > spec["max"]:
        return QualityFlag.INVALID, None

    if val > spec["extreme"]:
        # High pollution spike retained for event detection, flagged as SUSPECT
        return QualityFlag.SUSPECT, val

    return QualityFlag.VALID, val


def decompose_wind(wind_speed: Any, wind_dir_deg: Any) -> Tuple[float, float]:
    """
    Decomposes meteorological wind speed (m/s) and wind direction (degrees FROM which wind blows)
    into orthogonal U (East-West) and V (North-South) vectors:
    U = -wind_speed * sin(rad(dir))
    V = -wind_speed * cos(rad(dir))
    Calm winds (speed < 0.2 m/s) resolve to (0.0, 0.0).
    """
    if pd.isna(wind_speed) or wind_speed is None:
        return 0.0, 0.0

    try:
        speed = float(wind_speed)
    except (ValueError, TypeError):
        return 0.0, 0.0

    if speed < 0.2:
        return 0.0, 0.0

    if pd.isna(wind_dir_deg) or wind_dir_deg is None:
        return 0.0, 0.0

    try:
        deg = float(wind_dir_deg)
    except (ValueError, TypeError):
        return 0.0, 0.0

    rad = np.radians(deg)
    u_vec = float(-speed * np.sin(rad))
    v_vec = float(-speed * np.cos(rad))
    return round(u_vec, 4), round(v_vec, 4)


def decompose_wind_series(speed_series: pd.Series, dir_series: pd.Series) -> Tuple[pd.Series, pd.Series]:
    """
    Vectorized calculation of U and V wind components.
    """
    speed = pd.to_numeric(speed_series, errors="coerce").fillna(0.0)
    direction = pd.to_numeric(dir_series, errors="coerce").fillna(0.0)

    is_calm = speed < 0.2
    rad = np.radians(direction)

    u_vec = -speed * np.sin(rad)
    v_vec = -speed * np.cos(rad)

    u_vec = u_vec.mask(is_calm, 0.0).round(4)
    v_vec = v_vec.mask(is_calm, 0.0).round(4)

    return u_vec, v_vec