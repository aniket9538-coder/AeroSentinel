"""
AeroSentinel - Air Quality Cleaning & Preprocessing Pipeline
File: ai-service/preprocessing/air_quality.py

Production cleaning, range validation, gap limiting, and quality auditing
for CAAQMS / CPCB ground station air quality measurements.
"""

from typing import Dict, Optional, Tuple
import numpy as np
import pandas as pd
from app.schemas.canonical import QualityFlag


POLLUTANT_VALID_RANGES = {
    "pm25": (0.0, 1500.0, 500.0),
    "pm10": (0.0, 2000.0, 800.0),
    "no2": (0.0, 1000.0, 300.0),
    "so2": (0.0, 1000.0, 300.0),
    "co": (0.0, 100.0, 30.0),
    "o3": (0.0, 1000.0, 300.0),
}


def clean_air_quality_dataframe(
    df: pd.DataFrame,
    station_coord_map: Optional[Dict[str, Tuple[float, float]]] = None
) -> Tuple[pd.DataFrame, Dict[str, int]]:
    """
    Cleans raw air quality data according to F1-A requirements.
    
    Returns:
        Tuple of (cleaned_df, audit_stats_dict)
    """
    if df.empty:
        return pd.DataFrame(), {"raw_rows": 0, "cleaned_rows": 0}

    stats = {
        "raw_rows": len(df),
        "duplicates_removed": 0,
        "invalid_coords": 0,
        "negative_values_flagged": 0,
        "extreme_spikes_flagged": 0,
        "cleaned_rows": 0,
    }

    clean_df = df.copy()

    # Standardize column naming
    clean_df.columns = clean_df.columns.str.strip().str.lower().str.replace(".", "_", regex=False)
    if "pm2_5" in clean_df.columns and "pm25" not in clean_df.columns:
        clean_df.rename(columns={"pm2_5": "pm25"}, inplace=True)

    # Resolve timestamps
    ts_col = next((c for c in ["observed_at", "timestamp", "datetime", "date"] if c in clean_df.columns), None)
    if not ts_col:
        raise ValueError("Missing timestamp column in air quality input.")

    clean_df["observed_at_raw"] = clean_df[ts_col].astype(str)
    clean_df["observed_at"] = pd.to_datetime(clean_df[ts_col], utc=True, errors="coerce")
    clean_df = clean_df.dropna(subset=["observed_at"]).copy()

    # Round to hourly floor for deterministic canonical alignment
    clean_df["hourly_bin"] = clean_df["observed_at"].dt.floor("h")

    # Standardize station ID
    st_col = next((c for c in ["station_id", "station", "station_code"] if c in clean_df.columns), None)
    clean_df["station_id"] = clean_df[st_col].astype(str).str.strip() if st_col else "ST_UNKNOWN"

    # Deduplication: Deterministically keep the latest ingestion record per station/hour
    initial_len = len(clean_df)
    clean_df = clean_df.sort_values(by=["station_id", "hourly_bin"]).drop_duplicates(
        subset=["station_id", "hourly_bin"], keep="last"
    )
    stats["duplicates_removed"] = initial_len - len(clean_df)

    # Attach coordinates if missing from station catalog
    if station_coord_map:
        for st_id, (lat, lon) in station_coord_map.items():
            mask = clean_df["station_id"] == st_id
            if "latitude" not in clean_df.columns or clean_df["latitude"].isna().all():
                clean_df.loc[mask, "latitude"] = lat
                clean_df.loc[mask, "longitude"] = lon

    # Validate coordinate boundaries
    if "latitude" in clean_df.columns and "longitude" in clean_df.columns:
        invalid_mask = (
            clean_df["latitude"].isna()
            | clean_df["longitude"].isna()
            | (clean_df["latitude"] < -90.0)
            | (clean_df["latitude"] > 90.0)
            | (clean_df["longitude"] < -180.0)
            | (clean_df["longitude"] > 180.0)
        )
        stats["invalid_coords"] = int(invalid_mask.sum())
        clean_df = clean_df[~invalid_mask].copy()

    # Initialize canonical QualityFlag column
    clean_df["quality_flag"] = QualityFlag.VALID.value

    # Validate physical ranges for each pollutant
    for pol, (low, high, spike) in POLLUTANT_VALID_RANGES.items():
        if pol not in clean_df.columns:
            continue

        clean_df[pol] = pd.to_numeric(clean_df[pol], errors="coerce")

        # Negative reading check: sensor calibration error
        neg_mask = clean_df[pol] < low
        if neg_mask.any():
            stats["negative_values_flagged"] += int(neg_mask.sum())
            clean_df.loc[neg_mask, "quality_flag"] = QualityFlag.INVALID.value
            clean_df.loc[neg_mask, pol] = np.nan

        # Extreme spike check: preserve value for anomaly analysis, flag as SUSPECT
        spike_mask = clean_df[pol] > spike
        if spike_mask.any():
            stats["extreme_spikes_flagged"] += int(spike_mask.sum())
            clean_df.loc[spike_mask, "quality_flag"] = QualityFlag.SUSPECT.value

    # Gap handling: Apply forward-fill for isolated 1-hour gaps per station only
    clean_df = clean_df.sort_values(by=["station_id", "hourly_bin"]).reset_index(drop=True)
    if "pm25" in clean_df.columns:
        clean_df["pm25_clean"] = clean_df.groupby("station_id")["pm25"].transform(
            lambda s: s.interpolate(method="linear", limit=1)
        )
    else:
        clean_df["pm25_clean"] = np.nan

    stats["cleaned_rows"] = len(clean_df)
    return clean_df, stats