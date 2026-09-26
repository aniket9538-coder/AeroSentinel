"""
AeroSentinel - Hotspot Target Definition
File: ai-service/ml/hotspot/target.py

Derives the reproducible proxy target for potential emerging pollution hotspots:
A spatial anomaly where local PM2.5 exceeds NAAQS standard (60 ug/m3) AND 
exceeds the leave-one-out spatial neighbor mean by >= 15%.
"""

import numpy as np
import pandas as pd


def create_hotspot_target(
    df: pd.DataFrame,
    threshold_pm25: float = 60.0,
    anomaly_factor: float = 1.15
) -> pd.DataFrame:
    """
    Constructs binary target 'is_potential_hotspot'.
    Leakage Guard: Uses only concurrent observations at time T.
    """
    if df.empty or "pm25_clean" not in df.columns:
        return df

    out_df = df.copy()
    
    # Base concentration condition
    naaqs_exceedance = out_df["pm25_clean"] >= threshold_pm25
    
    # Local spatial anomaly condition
    if "pm25_spatial_lag_mean" in out_df.columns:
        spatial_anomaly = out_df["pm25_clean"] >= (out_df["pm25_spatial_lag_mean"] * anomaly_factor)
    else:
        spatial_anomaly = pd.Series(True, index=out_df.index)
        
    out_df["is_potential_hotspot"] = (naaqs_exceedance & spatial_anomaly).astype(int)
    return out_df