"""
AeroSentinel - Step 1/2/3: F2 Dataset & Feature Audit
Inspects real parquet datasets on disk for F3 modeling.
"""
from pathlib import Path
import pandas as pd
import numpy as np

repo_root = Path(__file__).resolve().parent.parent
data_dir = repo_root / "data" / "processed"

train_df = pd.read_parquet(data_dir / "train_dataset.parquet")
val_df = pd.read_parquet(data_dir / "val_dataset.parquet")
test_df = pd.read_parquet(data_dir / "test_dataset.parquet")

print("=" * 60)
print("1. DATASET PARTITION SIZES & TIMESTAMPS")
print("=" * 60)
for name, df in [("Train", train_df), ("Val", val_df), ("Test", test_df)]:
    ts_col = "observed_at" if "observed_at" in df.columns else "hourly_bin"
    t_min = df[ts_col].min()
    t_max = df[ts_col].max()
    print(f"{name:5s} | Records: {len(df):6,d} | Range: {t_min} -> {t_max}")

print("\n" + "=" * 60)
print("2. SPATIAL & CITY COVERAGE")
print("=" * 60)
full_df = pd.concat([train_df, val_df, test_df], ignore_index=True)
cities = full_df["city_id"].unique() if "city_id" in full_df.columns else "N/A"
stations = full_df["station_id"].nunique() if "station_id" in full_df.columns else "N/A"
h3_cells = full_df["h3_cell_id"].nunique() if "h3_cell_id" in full_df.columns else "N/A"
print(f"Cities: {cities}")
print(f"Unique Stations: {stations}")
print(f"Unique H3 (Res 8) Cells: {h3_cells}")

print("\n" + "=" * 60)
print("3. CANDIDATE TARGETS & IMBALANCE RATIOS")
print("=" * 60)
pm25_col = "pm25_clean" if "pm25_clean" in full_df.columns else "pm25"
print(f"Primary PM2.5 Column: '{pm25_col}'")
for name, df in [("Train", train_df), ("Val", val_df), ("Test", test_df)]:
    s = df[pm25_col].dropna()
    ge_60 = (s >= 60.0).sum()
    pct = (ge_60 / len(s)) * 100 if len(s) > 0 else 0
    print(f"{name:5s} | Total valid: {len(s):5,d} | >= 60 ug/m3: {ge_60:5,d} ({pct:.2f}%) | Mean: {s.mean():.2f} | Std: {s.std():.2f}")

print("\n" + "=" * 60)
print("4. FEATURE MATRIX & MISSINGNESS (<100% complete)")
print("=" * 60)
exclude = ["observed_at", "hourly_bin", "station_id", "city_id", "h3_cell_id", "pm25", "pm25_clean", "pm25_raw"]
feature_cols = [c for c in train_df.columns if c not in exclude and not c.startswith("target_")]
print(f"Total input features identified: {len(feature_cols)}")
null_counts = train_df[feature_cols].isnull().sum()
high_nulls = null_counts[null_counts > 0]
if len(high_nulls) == 0:
    print("Zero missing values across all candidate features in training set.")
else:
    for col, count in high_nulls.items():
        print(f" - {col}: {count:,} nulls ({count/len(train_df)*100:.1f}%)")