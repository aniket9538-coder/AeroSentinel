"""
AeroSentinel - Spatiotemporal Alignment & Feature Preparation
File: ai-service/preprocessing/alignment.py

Aligns air quality and weather datasets, derives cyclical temporal features,
constructs multi-horizon forecast targets, and enforces strict chronological splits.
"""

from typing import Dict, Tuple
import numpy as np
import pandas as pd


def align_air_and_weather(
    air_df: pd.DataFrame,
    weather_df: pd.DataFrame
) -> pd.DataFrame:
    """
    Spatially and temporally aligns cleaned air quality and weather observations.
    """
    if air_df.empty:
        return pd.DataFrame()

    if weather_df.empty:
        merged = air_df.copy()
        for c in ["temperature", "humidity", "wind_speed", "wind_u", "wind_v", "rainfall", "pressure"]:
            merged[c] = np.nan
        return merged

    # Align on hourly_bin and city_id
    if "city_id" in air_df.columns and "city_id" in weather_df.columns:
        merged = pd.merge(
            air_df,
            weather_df.drop(columns=["observed_at"], errors="ignore"),
            on=["hourly_bin", "city_id"],
            how="left",
            suffixes=("", "_wx")
        )
    else:
        merged = pd.merge(
            air_df,
            weather_df.drop(columns=["observed_at"], errors="ignore"),
            on=["hourly_bin"],
            how="left",
            suffixes=("", "_wx")
        )

    # Derive cyclical temporal features from local context
    local_ts = merged["observed_at"].dt.tz_convert("Asia/Kolkata")
    hour = local_ts.dt.hour
    dow = local_ts.dt.dayofweek

    merged["hour"] = hour
    merged["day_of_week"] = dow
    merged["is_weekend"] = (dow >= 5).astype(int)

    merged["hour_sin"] = np.round(np.sin(2 * np.pi * hour / 24.0), 4)
    merged["hour_cos"] = np.round(np.cos(2 * np.pi * hour / 24.0), 4)
    merged["dow_sin"] = np.round(np.sin(2 * np.pi * dow / 7.0), 4)
    merged["dow_cos"] = np.round(np.cos(2 * np.pi * dow / 7.0), 4)

    return merged


def construct_forecast_targets(
    df: pd.DataFrame,
    horizons: Tuple[int, ...] = (1, 3, 6),
    target_col: str = "pm25_clean"
) -> pd.DataFrame:
    """
    Constructs forward-looking supervised forecast targets strictly within each station.
    Targets are shifted backward so that at row T, the target value is y_{T+k}.
    """
    if df.empty or target_col not in df.columns:
        return df

    out_df = df.sort_values(by=["station_id", "hourly_bin"]).copy()

    for h in horizons:
        col_name = f"target_pm25_t_plus_{h}"
        out_df[col_name] = out_df.groupby("station_id")[target_col].shift(-h)

    return out_df


def split_dataset_chronologically(
    df: pd.DataFrame,
    train_frac: float = 0.70,
    val_frac: float = 0.15
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """
    Splits dataset chronologically to prevent temporal lookahead leakage.
    Train: [0, train_frac)
    Val:   [train_frac, train_frac + val_frac)
    Test:  [train_frac + val_frac, 1.0]
    """
    if df.empty:
        return pd.DataFrame(), pd.DataFrame(), pd.DataFrame()

    unique_times = df["hourly_bin"].drop_duplicates().sort_values().reset_index(drop=True)
    n_times = len(unique_times)

    if n_times < 3:
        # Fallback for minimal testing samples
        train_idx = int(len(df) * train_frac)
        val_idx = int(len(df) * (train_frac + val_frac))
        return df.iloc[:train_idx].copy(), df.iloc[train_idx:val_idx].copy(), df.iloc[val_idx:].copy()

    train_cutoff = unique_times.iloc[int(n_times * train_frac)]
    val_cutoff = unique_times.iloc[int(n_times * (train_frac + val_frac))]

    train_df = df[df["hourly_bin"] < train_cutoff].copy()
    val_df = df[(df["hourly_bin"] >= train_cutoff) & (df["hourly_bin"] < val_cutoff)].copy()
    test_df = df[df["hourly_bin"] >= val_cutoff].copy()

    return train_df, val_df, test_df