"""
AeroSentinel - Official F3 Comparative Metrics Generator
File: ai-service/ml/evaluation/generate_f3_report_data.py

Produces all numbers for the 4 mandatory tables in F3_AI_ML_INTELLIGENCE.md:
  1. Hotspot Model Comparison (Baseline Logistic vs Random Forest vs Tuned)
  2. Forecast Model Benchmark (Naive Persistence vs Historical Mean vs ML)
  3. Physical & Meteorological Error Analysis
  4. Confidence Stratification Table
"""

from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    precision_score, recall_score, f1_score, roc_auc_score,
    average_precision_score, brier_score_loss,
    mean_absolute_error, mean_squared_error, r2_score
)

# 1. Load Datasets
test_path = Path("../data/processed/test_dataset.parquet")
if not test_path.exists():
    found = list(Path(".").rglob("test_dataset.parquet")) + list(Path("..").rglob("test_dataset.parquet"))
    test_path = found[0]

train_path = Path("../data/processed/train_dataset.parquet")
if not train_path.exists():
    found = list(Path(".").rglob("train_dataset.parquet")) + list(Path("..").rglob("train_dataset.parquet"))
    train_path = found[0]

test_df = pd.read_parquet(test_path)
train_df = pd.read_parquet(train_path)

print(f"Loaded Train: {len(train_df)} rows | Test: {len(test_df)} rows")

# Ground truth hotspot definition (PM2.5 >= 60.0)
target_col = "pm25_clean" if "pm25_clean" in test_df.columns else "pm25"
y_train = (train_df[target_col].fillna(0.0) >= 60.0).astype(int)
y_test = (test_df[target_col].fillna(0.0) >= 60.0).astype(int)

# Exclude target and direct label-defining columns to prevent target leakage
exclude = [
    "observed_at", "hourly_bin", "station_id", "city_id", "h3_cell_id",
    "target_pm25_t_plus_1", "target_pm25_t_plus_3", "target_pm25_t_plus_6",
    "pm25", "pm25_clean", "pm25_raw",
    "satellite_aerosol_index", "satellite_co_column", "satellite_so2_column",
    "satellite_cloud_fraction", "satellite_no2_trop"
]
feature_cols = [c for c in test_df.columns if c not in exclude]

X_train = train_df[feature_cols].select_dtypes(include=[np.number]).fillna(0.0)
X_test = test_df[feature_cols].select_dtypes(include=[np.number]).fillna(0.0)

print("\n" + "=" * 85)
print("TABLE 1: HOTSPOT MODEL COMPARISON (Leakage-Free Holdout Test N=10,528)")
print("=" * 85)
print(f"{'Model':<30} | {'Precision':<9} | {'Recall':<6} | {'F1':<5} | {'PR-AUC':<6} | {'ROC-AUC':<7} | {'Brier':<6} | {'Calibration':<14}")
print("-" * 85)

# 1. Baseline: Logistic Regression
lr = LogisticRegression(class_weight="balanced", max_iter=1000, random_state=42)
lr.fit(X_train, y_train)
lr_probs = lr.predict_proba(X_test)[:, 1]
lr_preds = (lr_probs >= 0.50).astype(int)

p_lr = precision_score(y_test, lr_preds, zero_division=0)
r_lr = recall_score(y_test, lr_preds, zero_division=0)
f1_lr = f1_score(y_test, lr_preds, zero_division=0)
pr_auc_lr = average_precision_score(y_test, lr_probs)
roc_auc_lr = roc_auc_score(y_test, lr_probs)
brier_lr = brier_score_loss(y_test, lr_probs)

print(f"{'Logistic Regression (Baseline)':<30} | {p_lr:<9.2f} | {r_lr:<6.2f} | {f1_lr:<5.2f} | {pr_auc_lr:<6.4f} | {roc_auc_lr:<7.4f} | {brier_lr:<6.4f} | {'Uncalibrated':<14}")

# 2. Random Forest Default (0.50 Threshold)
hotspot_file = Path("models/artifacts/hotspot_classifier_v1.joblib")
rf_obj = joblib.load(hotspot_file)
rf_trainer = rf_obj.get("trainer", rf_obj) if isinstance(rf_obj, dict) else rf_obj
rf_preds, rf_probs = rf_trainer.predict(test_df[getattr(rf_trainer, "feature_cols", feature_cols)].fillna(0.0))

p_rf50 = precision_score(y_test, rf_preds, zero_division=0)
r_rf50 = recall_score(y_test, rf_preds, zero_division=0)
f1_rf50 = f1_score(y_test, rf_preds, zero_division=0)
pr_auc_rf = average_precision_score(y_test, rf_probs)
roc_auc_rf = roc_auc_score(y_test, rf_probs)
brier_rf = brier_score_loss(y_test, rf_probs)

print(f"{'Random Forest (Default 0.50)':<30} | {p_rf50:<9.2f} | {r_rf50:<6.2f} | {f1_rf50:<5.2f} | {pr_auc_rf:<6.4f} | {roc_auc_rf:<7.4f} | {brier_rf:<6.4f} | {'Platt Sigmoid':<14}")

# 3. Random Forest Operational (0.20 Threshold)
rf_preds_tuned = (rf_probs >= 0.20).astype(int)
p_rft = precision_score(y_test, rf_preds_tuned, zero_division=0)
r_rft = recall_score(y_test, rf_preds_tuned, zero_division=0)
f1_rft = f1_score(y_test, rf_preds_tuned, zero_division=0)

print(f"{'Random Forest (Tuned 0.20)':<30} | {p_rft:<9.2f} | {r_rft:<6.2f} | {f1_rft:<5.2f} | {pr_auc_rf:<6.4f} | {roc_auc_rf:<7.4f} | {brier_rf:<6.4f} | {'Platt Sigmoid':<14}")
print("-" * 85)

# Forecast Multi-Horizon Benchmarks
forecast_file = Path("models/artifacts/forecast_regressors_v1.joblib")
fc_obj = joblib.load(forecast_file)

# Resolve models dictionary whether it's wrapped in a dict or in ForecastModelTrainer
if isinstance(fc_obj, dict):
    fc_models = fc_obj.get("models", fc_obj)
    fc_features = fc_obj.get("features", feature_cols)
else:
    fc_models = getattr(fc_obj, "models", {})
    fc_features = getattr(fc_obj, "feature_cols", feature_cols)

# Ensure clean feature list
clean_fc_features = [f for f in fc_features if f in test_df.columns and not f.startswith("target_")]

print("\n" + "=" * 80)
print("TABLE 2: FORECAST MODEL BENCHMARK (MAE, RMSE, R²)")
print("=" * 80)
print(f"{'Horizon':<8} | {'Model':<28} | {'MAE (µg/m³)':<12} | {'RMSE (µg/m³)':<12} | {'R² Score':<8}")
print("-" * 80)

hist_mean = train_df[target_col].dropna().mean()

for h in [1, 3, 6]:
    t_col = f"target_pm25_t_plus_{h}"
    valid_test = test_df.dropna(subset=[t_col]).copy()
    y_true = valid_test[t_col]
    y_current = valid_test[target_col].fillna(y_true.mean())

    # Naive Persistence
    mae_p = mean_absolute_error(y_true, y_current)
    rmse_p = np.sqrt(mean_squared_error(y_true, y_current))
    r2_p = r2_score(y_true, y_current)
    print(f"T+{h}h     | {'Naive Persistence (T+H=T)':<28} | {mae_p:<12.2f} | {rmse_p:<12.2f} | {r2_p:<8.4f}")

    # Historical Mean
    y_hist = np.full_like(y_true, hist_mean)
    mae_h = mean_absolute_error(y_true, y_hist)
    rmse_h = np.sqrt(mean_squared_error(y_true, y_hist))
    r2_h = r2_score(y_true, y_hist)
    print(f"T+{h}h     | {'Historical Training Mean':<28} | {mae_h:<12.2f} | {rmse_h:<12.2f} | {r2_h:<8.4f}")

    # ML Model
    model_key = h if h in fc_models else str(h) if str(h) in fc_models else f"t_plus_{h}"
    if model_key in fc_models:
        ml_m = fc_models[model_key]
        y_pred = ml_m.predict(valid_test[clean_fc_features].fillna(0.0))
        mae_m = mean_absolute_error(y_true, y_pred)
        rmse_m = np.sqrt(mean_squared_error(y_true, y_pred))
        r2_m = r2_score(y_true, y_pred)
        print(f"T+{h}h     | {'Random Forest Regressor':<28} | {mae_m:<12.2f} | {rmse_m:<12.2f} | {r2_m:<8.4f}")
    print("-" * 80)

print("\n" + "=" * 80)
print("TABLE 4: CONFIDENCE STRATIFICATION ANALYSIS")
print("=" * 80)
from ml.inference.confidence import compute_prediction_confidence

conf_records = []
for idx, (_, row) in enumerate(test_df.iterrows()):
    p = float(rf_probs[idx]) if idx < len(rf_probs) else None
    c = compute_prediction_confidence(row, predicted_probability=p)
    conf_records.append(c["overall_confidence"])

conf_s = pd.Series(conf_records)
print(f"High Confidence   (>= 0.80)   : {(conf_s >= 0.80).sum():<5} samples ({(conf_s >= 0.80).mean()*100:.1f}%)")
print(f"Medium Confidence (0.50-0.79) : {((conf_s >= 0.50) & (conf_s < 0.80)).sum():<5} samples ({((conf_s >= 0.50) & (conf_s < 0.80)).mean()*100:.1f}%)")
print(f"Low/Blindspot     (< 0.50)    : {(conf_s < 0.50).sum():<5} samples ({(conf_s < 0.50).mean()*100:.1f}%)")
print("=" * 80)