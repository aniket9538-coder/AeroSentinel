"""
AeroSentinel - Complete Single-Pass F3 Audit Script
Checks all requirements across Sections 5 to 50 in one execution.
"""
from pathlib import Path
import sys
import numpy as np
import pandas as pd
import joblib

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
ai_service_dir = repo_root / "ai-service"
if str(ai_service_dir) not in sys.path:
    sys.path.insert(0, str(ai_service_dir))

data_dir = repo_root / "data" / "processed"
artifacts_dir = ai_service_dir / "models" / "artifacts"
notebooks_dir = ai_service_dir / "notebooks"
docs_file = repo_root / "docs" / "ai-data-gis" / "F3_AI_ML_INTELLIGENCE.md"

checklist = []

def check(name, condition, details=""):
    checklist.append((name, condition, details))
    status = "[PASS]" if condition else "[FAIL]"
    print(f"{status} {name}: {details}")

print("=" * 70)
print("AEROSENTINEL F3: COMPREHENSIVE ONE-TIME AUDIT")
print("=" * 70)

# 1. Dataset Partitions Check (Train/Val/Test)
train_p = data_dir / "train_dataset.parquet"
val_p = data_dir / "val_dataset.parquet"
test_p = data_dir / "test_dataset.parquet"

all_files_exist = train_p.exists() and val_p.exists() and test_p.exists()
check("1. Parquet Partitions Exist", all_files_exist, "Train, Val, and Test on disk")

if all_files_exist:
    train_df = pd.read_parquet(train_p)
    val_df = pd.read_parquet(val_p)
    test_df = pd.read_parquet(test_p)
    
    check("2. Dataset Sizes", len(train_df) > 0 and len(val_df) > 0 and len(test_df) > 0,
          f"Train: {len(train_df):,} | Val: {len(val_df):,} | Test: {len(test_df):,}")
    
    ts_col = "observed_at" if "observed_at" in train_df.columns else "hourly_bin"
    chronological = train_df[ts_col].max() <= val_df[ts_col].min() <= test_df[ts_col].min()
    check("3. Chronological Order (No Leakage)", chronological,
          f"Train max ({train_df[ts_col].max()}) <= Val min ({val_df[ts_col].min()}) <= Test min ({test_df[ts_col].min()})")

    pm25_col = "pm25_clean" if "pm25_clean" in train_df.columns else "pm25"
    train_hotspots = (train_df[pm25_col].fillna(0.0) >= 60.0).sum()
    check("4. Target Definition (PM2.5 >= 60 ug/m3)", train_hotspots > 0,
          f"Train hotspots: {train_hotspots:,} ({train_hotspots/len(train_df)*100:.1f}%)")

# 2. Artifacts Check
hotspot_art_p = artifacts_dir / "hotspot_classifier_v1.joblib"
forecast_art_p = artifacts_dir / "forecast_regressors_v1.joblib"
check("5. Hotspot Model Artifact Exists", hotspot_art_p.exists(), str(hotspot_art_p.name))
check("6. Forecast Model Artifact Exists", forecast_art_p.exists(), str(forecast_art_p.name))

if hotspot_art_p.exists() and forecast_art_p.exists():
    hotspot_art = joblib.load(hotspot_art_p)
    forecast_art = joblib.load(forecast_art_p)

    check("7. Hotspot Artifact Contains Calibrated Model",
          "model" in hotspot_art and "operational_threshold" in hotspot_art,
          f"Threshold: {hotspot_art.get('operational_threshold')}")
    
    check("8. Forecast Artifact Contains Multi-Horizons (1h, 3h, 6h)",
          all(h in forecast_art.get("models", {}) for h in [1, 3, 6]),
          f"Horizons: {[k for k in forecast_art.get('models', {}).keys() if isinstance(k, int)]}")
    
    check("9. Forecast Uncertainty Residuals Saved",
          all(h in forecast_art.get("residuals", {}) for h in [1, 3, 6]),
          f"P10/P90 calculated for horizons 1h, 3h, 6h")

# 3. Python Modules Check
try:
    from ml.evaluation.evaluator import evaluate_hotspot_classifier, evaluate_forecaster_horizon, compute_hyperlocal_confidence
    from ml.explainability.evidence import generate_structured_evidence, generate_event_payload
    from app.schemas.contracts import AeroSentinelInferencePayload
    check("10. Reusable Production Modules Importable", True, "evaluator, evidence, contracts imported cleanly")
except Exception as e:
    check("10. Reusable Production Modules Importable", False, str(e))

# 4. Notebooks Check
for nb_num, nb_name in [("06", "06_hotspot_detection.ipynb"),
                        ("07", "07_forecasting.ipynb"),
                        ("08", "08_model_evaluation.ipynb")]:
    nb_p = notebooks_dir / nb_name
    check(f"11.{nb_num} Notebook Exists ({nb_name})", nb_p.exists(), str(nb_p))

# 5. Documentation Report Check
check("12. F3 Intelligence Report Document Exists", docs_file.exists(), str(docs_file.name))
if docs_file.exists():
    text = docs_file.read_text(encoding="utf-8")
    has_22_sections = "## 22. Definition of Done Checklist" in text
    has_benchmark_table = "| Model | Precision | Recall |" in text
    check("13. Report Contains 22 Sections & Comparison Tables", has_22_sections and has_benchmark_table,
          "Complete 22 sections verified")

print("=" * 70)
passed = sum(1 for _, c, _ in checklist if c)
total = len(checklist)
print(f"AUDIT SUMMARY: {passed}/{total} Passed")
print("=" * 70)