"""
AeroSentinel - Step 1-3 & Section 20-23: Empirical Regime & Inspection Analysis
Computes dataset inventory, class distribution, and error analysis across
spatial and meteorological regimes on the test set.
"""
from pathlib import Path
import pandas as pd
import numpy as np
import joblib

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
data_dir = repo_root / "data" / "processed"
artifacts_dir = repo_root / "ai-service" / "models" / "artifacts"

train_df = pd.read_parquet(data_dir / "train_dataset.parquet")
val_df = pd.read_parquet(data_dir / "val_dataset.parquet")
test_df = pd.read_parquet(data_dir / "test_dataset.parquet")

print("=" * 70)
print("1. STEP 1 & 2: DATASET INVENTORY")
print("=" * 70)
for name, df in [("Train", train_df), ("Val", val_df), ("Test", test_df)]:
    ts_col = "observed_at" if "observed_at" in df.columns else "hourly_bin"
    print(f"{name:5s} | Records: {len(df):6,d} | Range: {df[ts_col].min()} -> {df[ts_col].max()}")

full_df = pd.concat([train_df, val_df, test_df], ignore_index=True)
h3_count = full_df["h3_cell_id"].nunique() if "h3_cell_id" in full_df.columns else 0
station_count = full_df["station_id"].nunique() if "station_id" in full_df.columns else 0
print(f"H3 Resolution 8 Cells: {h3_count} | Unique Stations: {station_count}")

pm25_col = "pm25_clean" if "pm25_clean" in full_df.columns else "pm25"
train_pos = (train_df[pm25_col].fillna(0.0) >= 60.0).sum()
val_pos = (val_df[pm25_col].fillna(0.0) >= 60.0).sum()
test_pos = (test_df[pm25_col].fillna(0.0) >= 60.0).sum()

print("\n" + "=" * 70)
print("2. STEP 3 & 4: HOTSPOT TARGET CLASS DISTRIBUTION (PM2.5 >= 60.0 ug/m3)")
print("=" * 70)
print(f"Train Positives: {train_pos:,} / {len(train_df):,} ({train_pos/len(train_df)*100:.2f}%)")
print(f"Val Positives:   {val_pos:,} / {len(val_df):,} ({val_pos/len(val_df)*100:.2f}%)")
print(f"Test Positives:  {test_pos:,} / {len(test_df):,} ({test_pos/len(test_df)*100:.2f}%)")

print("\n" + "=" * 70)
print("3. SECTION 20, 21, 23: SPATIAL & REGIME ERROR ANALYSIS (TEST SET)")
print("=" * 70)

forecast_art = joblib.load(artifacts_dir / "forecast_regressors_v1.joblib")
features = forecast_art["feature_cols"]
model_t1 = forecast_art["models"][1]

test_clean = test_df.dropna(subset=["target_pm25_t_plus_1", pm25_col]).copy()
test_clean["pred_t1"] = model_t1.predict(test_clean[features].fillna(0.0))
test_clean["abs_err"] = (test_clean["target_pm25_t_plus_1"] - test_clean["pred_t1"]).abs()

# Regime 1: Wind speed (Stagnant vs Moderate/High)
if "wind_speed" in test_clean.columns:
    stagnant = test_clean[test_clean["wind_speed"] < 1.0]
    breezy = test_clean[test_clean["wind_speed"] >= 1.0]
    print(f"Wind < 1.0 m/s (Stagnant):   N = {len(stagnant):5,d} | MAE = {stagnant['abs_err'].mean():.2f} ug/m3")
    print(f"Wind >= 1.0 m/s (Ventilated): N = {len(breezy):5,d} | MAE = {breezy['abs_err'].mean():.2f} ug/m3")

# Regime 2: Spatial Monitoring Density (Near Station vs Coverage Gap)
if "nearest_station_distance_km" in test_clean.columns:
    dense = test_clean[test_clean["nearest_station_distance_km"] <= 5.0]
    sparse = test_clean[test_clean["nearest_station_distance_km"] > 5.0]
    print(f"Nearest Station <= 5km:      N = {len(dense):5,d} | MAE = {dense['abs_err'].mean():.2f} ug/m3")
    print(f"Nearest Station > 5km (Gap): N = {len(sparse):5,d} | MAE = {sparse['abs_err'].mean():.2f} ug/m3")

# Regime 3: Diurnal Cycle (Day vs Night)
if "is_daytime" in test_clean.columns:
    day = test_clean[test_clean["is_daytime"] == 1]
    night = test_clean[test_clean["is_daytime"] == 0]
    print(f"Daytime:                     N = {len(day):5,d} | MAE = {day['abs_err'].mean():.2f} ug/m3")
    print(f"Nighttime:                   N = {len(night):5,d} | MAE = {night['abs_err'].mean():.2f} ug/m3")

print("=" * 70)