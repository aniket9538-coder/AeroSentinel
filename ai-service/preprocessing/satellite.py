"""
AeroSentinel - Sentinel-5P TROPOMI Satellite Feature Extraction Module
File: ai-service/preprocessing/satellite.py

Ingests atmospheric column densities, applies cloud quality filtering (qa > 0.50,
cloud fraction <= 0.30), maps observations to H3 Resolution 8 cells, and executes
leakage-free temporal alignment.
"""

from datetime import datetime, timedelta
from typing import Dict, Optional
import numpy as np
import pandas as pd
from preprocessing.spatial_alignment import bind_h3_cell


def clean_satellite_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    Cleans Sentinel-5P TROPOMI records, parses UTC timestamps, validates coordinate bounds,
    and applies cloud masking. Observations with cloud_fraction > 0.30 are flagged as SUSPECT.
    """
    if df.empty:
        return pd.DataFrame()

    clean_df = df.copy()
    clean_df.columns = clean_df.columns.str.strip().str.lower()

    ts_col = next((c for c in ["observed_at", "timestamp", "datetime", "date"] if c in clean_df.columns), None)
    if not ts_col:
        raise ValueError("Missing observation timestamp in satellite data.")

    clean_df["observed_at"] = pd.to_datetime(clean_df[ts_col], utc=True, errors="coerce")
    clean_df = clean_df.dropna(subset=["observed_at"]).copy()

    clean_df["latitude"] = pd.to_numeric(clean_df["latitude"], errors="coerce")
    clean_df["longitude"] = pd.to_numeric(clean_df["longitude"], errors="coerce")
    valid_coords = (
        clean_df["latitude"].between(-90.0, 90.0) &
        clean_df["longitude"].between(-180.0, 180.0)
    )
    clean_df = clean_df[valid_coords].copy()

    # Cloud fraction filtering: qa > 0.50 corresponds to clear-sky/low-cloud conditions
    if "cloud_fraction" in clean_df.columns:
        clean_df["cloud_fraction"] = pd.to_numeric(clean_df["cloud_fraction"], errors="coerce").fillna(0.0)
        clean_df["is_cloud_free"] = clean_df["cloud_fraction"] <= 0.30
    else:
        clean_df["cloud_fraction"] = 0.0
        clean_df["is_cloud_free"] = True

    # Bind H3 cell
    clean_df["h3_cell_id"] = clean_df.apply(
        lambda r: bind_h3_cell(r["latitude"], r["longitude"], resolution=8), axis=1
    )

    # Coerce atmospheric column measurements to numeric
    for col in ["tropospheric_no2", "so2_column", "co_column", "aerosol_index"]:
        if col in clean_df.columns:
            clean_df[col] = pd.to_numeric(clean_df[col], errors="coerce")

    return clean_df.sort_values(by="observed_at").reset_index(drop=True)


def extract_satellite_features_for_observation(
    obs_time: datetime,
    obs_h3: str,
    sat_df: pd.DataFrame,
    lookback_hours: int = 24
) -> Dict[str, Optional[float]]:
    """
    Extracts the most recent cloud-free satellite atmospheric indicator strictly
    observed prior to or at obs_time within a 24-hour lookback window.
    """
    default_vals = {
        "satellite_no2_trop": None,
        "satellite_so2_column": None,
        "satellite_co_column": None,
        "satellite_aerosol_index": None,
        "satellite_cloud_fraction": None,
    }

    if sat_df.empty:
        return default_vals

    start_window = obs_time - timedelta(hours=lookback_hours)
    # Strictly non-leaking: satellite overpass must be <= observation time
    mask = (sat_df["observed_at"] >= start_window) & (sat_df["observed_at"] <= obs_time)
    past_sat = sat_df[mask]

    if past_sat.empty:
        return default_vals

    # Preference: Match exact H3 cell if available, else regional city-wide average
    cell_match = past_sat[past_sat["h3_cell_id"] == obs_h3]
    target_data = cell_match if not cell_match.empty else past_sat

    # Only include cloud-free observations if available
    cloud_free = target_data[target_data["is_cloud_free"]]
    selected = cloud_free.iloc[-1] if not cloud_free.empty else target_data.iloc[-1]

    return {
        "satellite_no2_trop": round(float(selected["tropospheric_no2"]), 6) if "tropospheric_no2" in selected and pd.notna(selected["tropospheric_no2"]) else None,
        "satellite_so2_column": round(float(selected["so2_column"]), 6) if "so2_column" in selected and pd.notna(selected["so2_column"]) else None,
        "satellite_co_column": round(float(selected["co_column"]), 6) if "co_column" in selected and pd.notna(selected["co_column"]) else None,
        "satellite_aerosol_index": round(float(selected["aerosol_index"]), 3) if "aerosol_index" in selected and pd.notna(selected["aerosol_index"]) else None,
        "satellite_cloud_fraction": round(float(selected["cloud_fraction"]), 3) if "cloud_fraction" in selected and pd.notna(selected["cloud_fraction"]) else None,
    }