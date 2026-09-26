"""
AeroSentinel - Master Data & Spatial Feature Pipeline (F1 + F2)
File: ai-service/preprocessing/pipeline.py

Orchestrates raw cleaning, spatiotemporal alignment, fire extraction,
satellite enrichment, GIS proximity calculation, target construction,
and chronological dataset partitioning.
"""

from pathlib import Path
from typing import List, Tuple
import numpy as np
import pandas as pd

from app.utils.config import settings
from preprocessing.cleaning import clean_air_quality_data, clean_weather_data
from preprocessing.fire import clean_fire_dataframe, extract_fire_features_for_observation
from preprocessing.satellite import clean_satellite_dataframe, extract_satellite_features_for_observation
from preprocessing.spatial_features import (
    compute_spatial_lag,
    extract_monitoring_coverage_features,
    extract_gis_proximity_features,
)


def load_weather_csv(file_path: Path) -> pd.DataFrame:
    """
    Safely loads Open-Meteo weather CSV files by skipping the 3 metadata rows
    (latitude/longitude line, value line, empty line) so columns start at line 4.
    """
    if not file_path.exists():
        return pd.DataFrame()

    try:
        # Standard Open-Meteo CSV: row 0 metadata, row 1 values, row 2 blank -> row 3 headers
        return pd.read_csv(file_path, skiprows=3, encoding="utf-8")
    except UnicodeDecodeError:
        return pd.read_csv(file_path, skiprows=3, encoding="latin1")
    except Exception:
        # Fallback if file does not have standard Open-Meteo metadata rows
        return pd.read_csv(file_path)


def align_air_and_weather(air_df: pd.DataFrame, weather_df: pd.DataFrame) -> pd.DataFrame:
    """
    Fuses air quality with surface weather based on exact hourly bins and matching city_id.
    Prevents future leakage by enforcing that weather at time T is only matched to air quality at time T.
    Derives cyclical diurnal and weekly temporal features.
    """
    if air_df.empty:
        return pd.DataFrame()

    w_cols = [
        "hourly_bin", "city_id", "temperature", "humidity", 
        "wind_speed", "wind_direction", "wind_u", "wind_v", "rainfall", "pressure"
    ]
    avail_w_cols = [c for c in w_cols if c in weather_df.columns]
    
    if not weather_df.empty and "hourly_bin" in weather_df.columns:
        w_subset = weather_df[avail_w_cols].drop_duplicates(subset=["hourly_bin", "city_id"])
        merged = pd.merge(
            air_df, 
            w_subset, 
            on=["hourly_bin", "city_id"], 
            how="left",
            suffixes=("", "_weather")
        )
    else:
        merged = air_df.copy()
        for col in ["temperature", "humidity", "wind_speed", "wind_direction", "wind_u", "wind_v", "rainfall", "pressure"]:
            merged[col] = np.nan

    # Derive cyclical temporal features without lookahead
    if "observed_at" in merged.columns:
        local_ts = pd.to_datetime(merged["observed_at"], utc=True).dt.tz_convert("Asia/Kolkata")
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


def enrich_with_fire_and_satellite(
    df: pd.DataFrame,
    fires_df: pd.DataFrame,
    satellite_df: pd.DataFrame
) -> pd.DataFrame:
    """
    Fast hourly-batched feature enrichment for active fire and satellite observations.
    Calculates features once per unique (hourly_bin, station_id) to eliminate 95%+ redundant loops.
    """
    if df.empty:
        return df

    out_df = df.copy()

    # Pre-sort fires_df by detected_at for fast time slice searching
    if not fires_df.empty and "detected_at" in fires_df.columns:
        fires_df = fires_df.sort_values(by="detected_at").reset_index(drop=True)

    cache = {}
    fire_records = []
    sat_records = []

    print(f" -> Enriched lookups for {len(out_df)} observations with temporal caching...")

    for _, row in out_df.iterrows():
        obs_time = pd.to_datetime(row["observed_at"], utc=True).to_pydatetime()
        ws = float(row.get("wind_speed", 0.0)) if pd.notna(row.get("wind_speed")) else 0.0
        wd = float(row.get("wind_direction", 0.0)) if pd.notna(row.get("wind_direction")) else 0.0
        h3_cell = str(row.get("h3_cell_id", ""))
        cache_key = (str(row.get("hourly_bin")), str(row.get("station_id", "")))

        if cache_key in cache:
            f_feats, s_feats = cache[cache_key]
        else:
            # 1. Fire features (24-hour lookback, 25km radius, wind-aligned advection)
            f_feats = extract_fire_features_for_observation(
                obs_lat=row["latitude"],
                obs_lon=row["longitude"],
                obs_time=obs_time,
                wind_speed=ws,
                wind_direction=wd,
                fires_df=fires_df,
                lookback_hours=24,
                radius_km=25.0
            )
            # 2. Satellite atmospheric features (24-hour lookback, cloud quality filter)
            s_feats = extract_satellite_features_for_observation(
                obs_time=obs_time,
                obs_h3=h3_cell,
                sat_df=satellite_df,
                lookback_hours=24
            )
            cache[cache_key] = (f_feats, s_feats)

        fire_records.append(f_feats)
        sat_records.append(s_feats)

    fire_features_df = pd.DataFrame(fire_records, index=out_df.index)
    sat_features_df = pd.DataFrame(sat_records, index=out_df.index)

    return pd.concat([out_df, fire_features_df, sat_features_df], axis=1)


def generate_forecast_targets(
    df: pd.DataFrame,
    horizons: List[int] = [1, 3, 6],
    target_col: str = "pm25_clean"
) -> pd.DataFrame:
    """
    Constructs future prediction targets for PM2.5 forecasting (T+1, T+3, T+6 hours ahead).
    Targets are generated strictly per monitoring station and shifted backward so that
    row T contains y_{T+k}. Future target values are never placed into feature columns.
    """
    if df.empty or target_col not in df.columns:
        return df

    out_df = df.sort_values(by=["station_id", "hourly_bin"]).copy()

    for h in horizons:
        col_name = f"target_pm25_t_plus_{h}"
        out_df[col_name] = out_df.groupby("station_id")[target_col].shift(-h)

    return out_df


def split_chronologically(
    df: pd.DataFrame,
    train_ratio: float = 0.70,
    val_ratio: float = 0.15
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """
    Executes strict chronological splitting to prevent time-series lookahead leakage.
    Train: Earliest 70% of time window
    Validation: Next 15% of time window
    Test: Final 15% holdout test window
    """
    if df.empty:
        return pd.DataFrame(), pd.DataFrame(), pd.DataFrame()

    unique_times = df["hourly_bin"].drop_duplicates().sort_values().reset_index(drop=True)
    n_times = len(unique_times)

    if n_times < 3:
        train_idx = max(1, int(len(df) * train_ratio))
        val_idx = max(train_idx + 1, int(len(df) * (train_ratio + val_ratio)))
        return df.iloc[:train_idx].copy(), df.iloc[train_idx:val_idx].copy(), df.iloc[val_idx:].copy()

    train_cutoff_idx = max(1, int(n_times * train_ratio))
    val_cutoff_idx = max(train_cutoff_idx + 1, int(n_times * (train_ratio + val_ratio)))
    val_cutoff_idx = min(val_cutoff_idx, n_times - 1)

    train_time_max = unique_times.iloc[train_cutoff_idx - 1]
    val_time_max = unique_times.iloc[val_cutoff_idx - 1]

    train_df = df[df["hourly_bin"] <= train_time_max].copy()
    val_df = df[(df["hourly_bin"] > train_time_max) & (df["hourly_bin"] <= val_time_max)].copy()
    test_df = df[df["hourly_bin"] > val_time_max].copy()

    return train_df, val_df, test_df


def run_pipeline() -> None:
    """
    Executes the complete Phase 2 (F2) feature engineering pipeline with real raw data.
    """
    print("=" * 68)
    print("AERO-SENTINEL: Phase 2 (F2) Spatial Feature Engineering Pipeline")
    print("=" * 68)

    # Resolve raw data directory (supports ai-service/data/raw or root data/raw)
    candidate_raw_dirs = [
        Path("data/raw"),
        Path("../data/raw"),
        Path("aerosentinel/data/raw")
    ]
    raw_dir = next((d for d in candidate_raw_dirs if d.exists()), Path("data/raw"))
    sample_dir = settings.SAMPLE_DATA_DIR
    geo_dir = settings.GEO_DATA_DIR
    interim_dir = settings.INTERIM_DATA_DIR
    processed_dir = settings.PROCESSED_DATA_DIR

    interim_dir.mkdir(parents=True, exist_ok=True)
    processed_dir.mkdir(parents=True, exist_ok=True)

    # 1. Ingest and Clean Air Quality Data (Katraj Dairy Ground Station)
    raw_air_path = raw_dir / "cpcb_katraj.csv"
    if not raw_air_path.exists():
        raw_air_path = sample_dir / "air_quality.csv"
    print(f" -> Reading Air Quality from: {raw_air_path}")
    raw_air_df = pd.read_csv(raw_air_path)
    clean_air_df = clean_air_quality_data(raw_air_df, geo_dir / "monitoring_stations.geojson")
    air_interim_path = interim_dir / "air_quality_cleaned.parquet"
    clean_air_df.to_parquet(air_interim_path, index=False)
    print(f" [PASS] Cleaned Air Quality: {len(clean_air_df)} rows saved to {air_interim_path.name}")

    # 2. Ingest and Clean Weather Data (Open-Meteo Pune)
    raw_weather_path = raw_dir / "weather_pune.csv"
    if not raw_weather_path.exists():
        raw_weather_path = sample_dir / "weather.csv"
    print(f" -> Reading Weather from:     {raw_weather_path}")
    raw_weather_df = load_weather_csv(raw_weather_path)
    clean_weather_df = clean_weather_data(raw_weather_df)
    weather_interim_path = interim_dir / "weather_cleaned.parquet"
    clean_weather_df.to_parquet(weather_interim_path, index=False)
    print(f" [PASS] Cleaned Weather:     {len(clean_weather_df)} rows saved to {weather_interim_path.name}")

    # 3. Ingest and Clean Fire Data (NASA FIRMS VIIRS)
    fire_csv_path = raw_dir / "firms_viirs.csv"
    if not fire_csv_path.exists():
        fire_csv_path = sample_dir / "fire.csv"
    print(f" -> Reading Active Fire from: {fire_csv_path}")
    raw_fire_df = pd.read_csv(fire_csv_path) if fire_csv_path.exists() else pd.DataFrame()
    clean_fire_df = clean_fire_dataframe(raw_fire_df)
    fire_interim_path = interim_dir / "fire_cleaned.parquet"
    clean_fire_df.to_parquet(fire_interim_path, index=False)
    print(f" [PASS] Cleaned Active Fire: {len(clean_fire_df)} detections saved to {fire_interim_path.name}")

    # 4. Ingest and Clean Satellite Data (Sentinel-5P TROPOMI)
    sat_csv_path = raw_dir / "satellite_s5p.csv"
    if not sat_csv_path.exists():
        sat_csv_path = sample_dir / "satellite.csv"
    print(f" -> Reading Satellite from:   {sat_csv_path}")
    raw_sat_df = pd.read_csv(sat_csv_path) if sat_csv_path.exists() else pd.DataFrame()
    clean_sat_df = clean_satellite_dataframe(raw_sat_df)
    sat_interim_path = interim_dir / "satellite_cleaned.parquet"
    clean_sat_df.to_parquet(sat_interim_path, index=False)
    print(f" [PASS] Cleaned Satellite:   {len(clean_sat_df)} records saved to {sat_interim_path.name}")

    # 5. Spatiotemporal Alignment
    print(" -> Aligning Air Quality and Weather on hourly spatial bins...")
    aligned_df = align_air_and_weather(clean_air_df, clean_weather_df)
    print(f" [PASS] Base Aligned Records: {len(aligned_df)} rows matched.")

    # 6. Spatial Context & GIS Proximity Features
    print(" -> Computing Spatial Lag, Monitoring Coverage, and GIS Proximity...")
    target_col = "pm25_clean" if "pm25_clean" in aligned_df.columns else "pm25"
    spatial_df = compute_spatial_lag(aligned_df, target_col=target_col)
    spatial_df = extract_monitoring_coverage_features(spatial_df, geo_dir / "monitoring_stations.geojson")
    spatial_df = extract_gis_proximity_features(spatial_df, geo_dir)
    print(" [PASS] Spatial Context Features Enriched.")

    # 7. Enrich with Fire & Satellite Remote-Sensing Features
    print(" -> Fusing Active Fire advection and Satellite atmospheric column indicators...")
    enriched_df = enrich_with_fire_and_satellite(spatial_df, clean_fire_df, clean_sat_df)
    print(" [PASS] Fire and Satellite Features Enriched.")

    # 8. Generate Supervised Multi-Horizon Forecast Targets
    print(" -> Constructing forecast targets (T+1, T+3, T+6)...")
    modeled_df = generate_forecast_targets(enriched_df, horizons=[1, 3, 6], target_col=target_col)
    master_parquet_path = processed_dir / "aligned_environmental.parquet"
    modeled_df.to_parquet(master_parquet_path, index=False)
    print(f" [PASS] Master Feature Matrix: {modeled_df.shape[0]} rows x {modeled_df.shape[1]} columns -> {master_parquet_path.name}")

    # 9. Chronological Train / Val / Test Split
    print(" -> Splitting datasets chronologically (70% Train, 15% Val, 15% Test)...")
    train_df, val_df, test_df = split_chronologically(modeled_df, train_ratio=0.70, val_ratio=0.15)

    train_path = processed_dir / "train_dataset.parquet"
    val_path = processed_dir / "val_dataset.parquet"
    test_path = processed_dir / "test_dataset.parquet"

    train_df.to_parquet(train_path, index=False)
    val_df.to_parquet(val_path, index=False)
    test_df.to_parquet(test_path, index=False)

    print(f"\nChronological Split Partitions:")
    print(f"   Train Set: {len(train_df)} rows -> {train_path.name}")
    print(f"   Val Set:   {len(val_df)} rows -> {val_path.name}")
    print(f"   Test Set:  {len(test_df)} rows -> {test_path.name}")
    print("=" * 68)
    print("[F2 PIPELINE] Feature Engineering completed successfully.")
    print("=" * 68)


# Backward compatibility alias
run_f1_pipeline = run_pipeline

if __name__ == "__main__":
    run_pipeline()