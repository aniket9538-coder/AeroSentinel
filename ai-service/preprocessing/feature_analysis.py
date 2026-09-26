"""
AeroSentinel - Feature Profiling & Correlation Analysis
File: ai-service/preprocessing/feature_analysis.py

Computes distribution statistics, missingness ratios, and collinearity matrices
across the engineered F2 feature dataset.
"""

from pathlib import Path
import numpy as np
import pandas as pd
from app.utils.config import settings


def analyze_f2_features() -> pd.DataFrame:
    parquet_path = settings.PROCESSED_DATA_DIR / "aligned_environmental.parquet"
    if not parquet_path.exists():
        print(f"[ERROR] {parquet_path} does not exist. Run preprocessing.pipeline first.")
        return pd.DataFrame()

    df = pd.read_parquet(parquet_path)
    print("=" * 70)
    print("AERO-SENTINEL: F2 Feature Profile & Collinearity Audit")
    print(f"Dataset Shape: {df.shape[0]} rows x {df.shape[1]} columns")
    print("=" * 70)

    # Identifiers and non-feature columns to exclude
    excluded = [
        "observation_id", "source", "source_record_id", "observed_at", "hourly_bin",
        "city_id", "station_id", "station_name", "h3_cell_id", "data_status",
        "quality_flag", "weather_quality_flag"
    ]
    
    # Select numeric features safely via Pandas
    all_num_cols = df.select_dtypes(include=[np.number]).columns.tolist()
    num_cols = [c for c in all_num_cols if c not in excluded]

    stats_records = []
    for col in num_cols:
        s = df[col]
        n_missing = int(s.isna().sum())
        pct_missing = round(n_missing / len(df) * 100.0, 1)
        valid_s = s.dropna()

        stats_records.append({
            "feature": col,
            "count": int(len(valid_s)),
            "missing_pct": pct_missing,
            "min": round(float(valid_s.min()), 4) if not valid_s.empty else np.nan,
            "mean": round(float(valid_s.mean()), 4) if not valid_s.empty else np.nan,
            "median": round(float(valid_s.median()), 4) if not valid_s.empty else np.nan,
            "max": round(float(valid_s.max()), 4) if not valid_s.empty else np.nan,
            "std": round(float(valid_s.std()), 4) if len(valid_s) > 1 else 0.0
        })

    stats_df = pd.DataFrame(stats_records)

    # Collinearity Analysis: Identify feature pairs with |r| >= 0.85
    corr_matrix = df[num_cols].corr()
    high_corr_pairs = []
    for i in range(len(num_cols)):
        for j in range(i + 1, len(num_cols)):
            c1 = num_cols[i]
            c2 = num_cols[j]
            val = corr_matrix.loc[c1, c2]
            if pd.notna(val) and abs(val) >= 0.85:
                high_corr_pairs.append((c1, c2, round(float(val), 3)))

    print(f"\nTotal Numerical Features Analyzed: {len(num_cols)}")
    sparse_cols = stats_df[stats_df["missing_pct"] > 50]["feature"].tolist()
    print(f"Features with > 50% missing values: {sparse_cols if sparse_cols else 'None'}")
    
    print("\nHighly Collinear Feature Pairs (|r| >= 0.85):")
    if high_corr_pairs:
        for c1, c2, r in high_corr_pairs[:12]:
            print(f"  - {c1} <--> {c2} (r = {r})")
        if len(high_corr_pairs) > 12:
            print(f"  ... and {len(high_corr_pairs) - 12} more pairs.")
    else:
        print("  None detected.")

    print("=" * 70)
    print("[F2 AUDIT COMPLETE] All feature statistics and collinearity verified.")
    print("=" * 70)

    return stats_df


if __name__ == "__main__":
    analyze_f2_features()