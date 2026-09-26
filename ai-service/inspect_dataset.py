"""
AeroSentinel - Step 1 & 2 F0/F1/F2 Dataset Inspection
File: ai-service/inspect_dataset.py
"""

from pathlib import Path
import pandas as pd

# Locate aligned environmental dataset
dataset_path = Path("../data/processed/aligned_environmental.parquet")
if not dataset_path.exists():
    found = list(Path(".").rglob("aligned_environmental.parquet")) + list(Path("..").rglob("aligned_environmental.parquet"))
    if found:
        dataset_path = found[0]
    else:
        raise FileNotFoundError("Could not find 'aligned_environmental.parquet'.")

print("=" * 70)
print(f"AEROSENTINEL F2 DATASET AUDIT: {dataset_path.resolve()}")
print("=" * 70)

df = pd.read_parquet(dataset_path)

print(f"Total Rows:     {len(df):,}")
print(f"Total Features: {len(df.columns)}")

# Temporal Coverage
if "observed_at" in df.columns:
    print(f"Time Range:     {df['observed_at'].min()}  -->  {df['observed_at'].max()}")

# Spatial & Station Coverage
if "city_id" in df.columns:
    print(f"Cities:         {list(df['city_id'].unique())}")
if "station_id" in df.columns:
    stations = list(df["station_id"].unique())
    print(f"Total Stations: {len(stations)} ({stations[:6]}...)")
if "h3_cell_id" in df.columns:
    print(f"Unique H3 Cells:{df['h3_cell_id'].nunique()}")

# Target Variables Check
print("\nTarget Candidate Audit:")
for h in [1, 3, 6]:
    col = f"target_pm25_t_plus_{h}"
    if col in df.columns:
        valid_cnt = df[col].notna().sum()
        print(f"  • {col:<22}: {valid_cnt:,} valid values ({valid_cnt/len(df)*100:.1f}%)")

# Missingness Breakdown
print("\nTop Missing Columns:")
missing = df.isnull().sum()
missing = missing[missing > 0].sort_values(ascending=False)
if len(missing) == 0:
    print("  No missing values detected.")
else:
    for col, count in missing.head(12).items():
        print(f"  • {col:<32}: {count:,} missing ({count/len(df)*100:.2f}%)")

# Class Distribution for Potential Hotspots (PM2.5 >= 60 ug/m3)
pm25_col = "pm25_clean" if "pm25_clean" in df.columns else "pm25"
if pm25_col in df.columns:
    hotspots = (df[pm25_col] >= 60.0).sum()
    print("\nInitial Label Balance Check (PM2.5 >= 60 µg/m³ Regulatory Standard):")
    print(f"  • Clean / Moderate (< 60 µg/m³): {len(df)-hotspots:,} ({(len(df)-hotspots)/len(df)*100:.2f}%)")
    print(f"  • Severe / Hotspot (>= 60 µg/m³): {hotspots:,} ({hotspots/len(df)*100:.2f}%)")
print("=" * 70)