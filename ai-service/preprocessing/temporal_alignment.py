"""
AeroSentinel - Temporal Alignment & Normalization
Enforces ISO 8601 UTC-aware timestamps, deterministic hourly binning,
and extracts cyclical ML feature representations.
"""
from datetime import datetime, timezone
from typing import Any, Optional, Union
import numpy as np
import pandas as pd


def normalize_timestamp(
    ts_val: Any, 
    source_tz: str = "Asia/Kolkata"
) -> Optional[pd.Timestamp]:
    """
    Parses datetime strings/objects, assigns source timezone if naive, 
    and converts to canonical UTC timezone-aware pd.Timestamp.
    """
    if pd.isna(ts_val) or ts_val is None or str(ts_val).strip() == "":
        return None
    
    try:
        ts = pd.to_datetime(ts_val)
        if ts.tzinfo is None:
            ts = ts.tz_localize(source_tz, ambiguous="NaT", nonexistent="shift_forward")
        return ts.tz_convert("UTC")
    except Exception:
        return None


def normalize_timestamp_series(
    series: pd.Series, 
    source_tz: str = "Asia/Kolkata"
) -> pd.Series:
    """
    Vectorized normalization of an entire timestamp Series to UTC.
    """
    parsed = pd.to_datetime(series, errors="coerce")
    if parsed.dt.tz is None:
        parsed = parsed.dt.tz_localize(source_tz, ambiguous="NaT", nonexistent="shift_forward")
    return parsed.dt.tz_convert("UTC")


def align_to_hourly_bin(
    df: pd.DataFrame, 
    timestamp_col: str = "observed_at",
    bin_col: str = "hourly_bin",
    source_tz: str = "Asia/Kolkata"
) -> pd.DataFrame:
    """
    Rounds timestamps to the nearest floor hour (T) to allow multi-source fusion.
    Extracts explicit temporal & cyclical trigonometric attributes for ML models.
    """
    df = df.copy()
    
    # Ensure column is datetime
    if not pd.api.types.is_datetime64_any_dtype(df[timestamp_col]):
        df[timestamp_col] = pd.to_datetime(df[timestamp_col], errors="coerce")

    # Ensure timezone-aware in UTC
    if df[timestamp_col].dt.tz is None:
        df[timestamp_col] = df[timestamp_col].dt.tz_localize(
            source_tz, ambiguous="NaT", nonexistent="shift_forward"
        ).dt.tz_convert("UTC")
    elif str(df[timestamp_col].dt.tz) != "UTC":
        df[timestamp_col] = df[timestamp_col].dt.tz_convert("UTC")

    # Floor to hourly bin resolution in UTC
    df[bin_col] = df[timestamp_col].dt.floor("h")

    # Extract temporal context in local time for human-interpretable analysis
    local_ts = df[timestamp_col].dt.tz_convert(source_tz)
    df["date"] = local_ts.dt.strftime("%Y-%m-%d")
    df["hour"] = local_ts.dt.hour
    df["day_of_week"] = local_ts.dt.dayofweek
    df["is_weekend"] = df["day_of_week"].isin([5, 6]).astype(int)

    # Cyclical encodings (prevents 23:00 and 00:00 discontinuity in models)
    df["hour_sin"] = np.sin(2 * np.pi * df["hour"] / 24.0)
    df["hour_cos"] = np.cos(2 * np.pi * df["hour"] / 24.0)
    df["dow_sin"] = np.sin(2 * np.pi * df["day_of_week"] / 7.0)
    df["dow_cos"] = np.cos(2 * np.pi * df["day_of_week"] / 7.0)

    return df