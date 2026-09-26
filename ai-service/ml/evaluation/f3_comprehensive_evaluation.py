"""
AeroSentinel - F3 Comprehensive Model Evaluation & Error Analysis
File: ai-service/ml/evaluation/f3_comprehensive_evaluation.py

Executes:
  1. Persistence & Historical Mean Baselines vs. ML Models (MAE, RMSE, R²)
  2. Spatial, Meteorological, and Pollution Regime Error Analysis
  3. Prediction Interval / Uncertainty Estimation (P10 - P90)
  4. Composite Confidence Calculation
"""

from pathlib import Path
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score, classification_report, roc_auc_score, brier_score_loss

# 1. Load Datasets
test_path = Path("../data/processed/test_dataset.parquet")
if not test_path.exists():
    found = list(Path(".").rglob("test_dataset.parquet")) + list(Path("..").rglob("test_dataset.parquet"))
    test_path = found[0]

test_df = pd.read_parquet(test_path)
print(f"Loaded {len(test_df)} test samples from {test_path.name}")

# Clean and ensure numeric types for satellite/weather columns
for c in test_df.columns:
    if "satellite_" in c:
        test_df[c] = pd.to_numeric(test_df[c], errors="coerce").fillna(0.0)

# Identify base pm2.5 column
pm25_base_col = "pm25_clean" if "pm25_clean" in test_df.columns else "pm25"
current_pm25 = test_df[pm25_base_col].fillna(test_df[pm25_base_col].median())

# 2. Forecaster Evaluation with Baselines
forecast_artifact = joblib.load("models/artifacts/forecast_regressors_v1.joblib")
models = forecast_artifact.get("models", forecast_artifact) if isinstance(forecast_artifact, dict) else forecast_artifact.models
features = forecast_artifact.get("features", None) if isinstance(forecast_artifact, dict) else getattr(forecast_artifact, "feature_cols", None)

if features is None:
    features = [c for c in test_df.columns if not c.startswith("target_") and c not in ["observed_at", "hourly_bin", "station_id", "city_id", "h3_cell_id"]]
clean_features = [f for f in features if f in test_df.columns and not f.startswith("target_")]

print("\n" + "=" * 70)
print("FORECAST BENCHMARK: BASELINES VS. MACHINE LEARNING")
print("=" * 70)
print(f"{'Horizon':<8} | {'Model':<22} | {'MAE (µg/m³)':<12} | {'RMSE (µg/m³)':<13} | {'R² Score':<8}")
print("-" * 70)

forecast_results = {}
for h in [1, 3, 6]:
    target_col = f"target_pm25_t_plus_{h}"
    model_key = h if h in models else str(h) if str(h) in models else f"t_plus_{h}"
    
    if target_col in test_df.columns and model_key in models:
        valid_df = test_df.dropna(subset=[target_col]).copy()
        y_true = valid_df[target_col]
        y_current = valid_df[pm25_base_col].fillna(y_true.mean())
        
        # Baseline 1: Naive Persistence (T+H = T)
        mae_pers = mean_absolute_error(y_true, y_current)
        rmse_pers = np.sqrt(mean_squared_error(y_true, y_current))
        r2_pers = r2_score(y_true, y_current)
        print(f"T+{h}h     | Naive Persistence      | {mae_pers:<12.2f} | {rmse_pers:<13.2f} | {r2_pers:<8.4f}")
        
        # Baseline 2: Historical Mean
        y_hist = np.full_like(y_true, y_true.mean())
        mae_hist = mean_absolute_error(y_true, y_hist)
        rmse_hist = np.sqrt(mean_squared_error(y_true, y_hist))
        r2_hist = r2_score(y_true, y_hist)
        print(f"T+{h}h     | Historical Mean        | {mae_hist:<12.2f} | {rmse_hist:<13.2f} | {r2_hist:<8.4f}")
        
        # ML Model (Random Forest Regressor)
        ml_model = models[model_key]
        X_eval = valid_df[clean_features].fillna(0.0)
        y_pred = ml_model.predict(X_eval)
        mae_ml = mean_absolute_error(y_true, y_pred)
        rmse_ml = np.sqrt(mean_squared_error(y_true, y_pred))
        r2_ml = r2_score(y_true, y_pred)
        print(f"T+{h}h     | Random Forest Regressor| {mae_ml:<12.2f} | {rmse_ml:<13.2f} | {r2_ml:<8.4f}")
        print("-" * 70)
        
        # Residuals for uncertainty bounds
        residuals = y_true - y_pred
        q10, q90 = np.percentile(residuals, 10), np.percentile(residuals, 90)
        
        forecast_results[f"T+{h}h"] = {
            "mae_ml": mae_ml, "rmse_ml": rmse_ml, "r2_ml": r2_ml,
            "mae_persistence": mae_pers, "rmse_persistence": rmse_pers, "r2_persistence": r2_pers,
            "error_q10": float(q10), "error_q90": float(q90)
        }

# 3. Contextual Error Analysis (Regimes)
print("\n" + "=" * 70)
print("ERROR ANALYSIS BY PHYSICAL & METEOROLOGICAL REGIME (T+1h Forecast)")
print("=" * 70)

target_col = "target_pm25_t_plus_1"
model_key = 1 if 1 in models else "1" if "1" in models else "t_plus_1"
valid_df = test_df.dropna(subset=[target_col]).copy()
y_true = valid_df[target_col]
y_pred = models[model_key].predict(valid_df[clean_features].fillna(0.0))
valid_df["abs_error"] = np.abs(y_true - y_pred)

# Wind Regime Analysis
if "wind_speed" in valid_df.columns:
    stagnant = valid_df[valid_df["wind_speed"] <= 2.0]["abs_error"].mean()
    ventilated = valid_df[valid_df["wind_speed"] > 5.0]["abs_error"].mean()
    print(f"Stagnant Air Regime (Wind <= 2.0 km/h)  : MAE = {stagnant:.2f} µg/m³ (N={len(valid_df[valid_df['wind_speed'] <= 2.0])})")
    print(f"Ventilated Regime   (Wind > 5.0 km/h)   : MAE = {ventilated:.2f} µg/m³ (N={len(valid_df[valid_df['wind_speed'] > 5.0])})")

# Fire Proximity Analysis
if "nearest_fire_distance_km" in valid_df.columns:
    near_fire = valid_df[valid_df["nearest_fire_distance_km"] <= 25.0]["abs_error"].mean()
    far_fire = valid_df[valid_df["nearest_fire_distance_km"] > 25.0]["abs_error"].mean()
    print(f"Near Active Thermal Fire (<= 25 km)     : MAE = {near_fire:.2f} µg/m³ (N={len(valid_df[valid_df['nearest_fire_distance_km'] <= 25.0])})")
    print(f"Distal Fire (> 25 km)                   : MAE = {far_fire:.2f} µg/m³ (N={len(valid_df[valid_df['nearest_fire_distance_km'] > 25.0])})")

# Pollution Severity Analysis
clean_env = valid_df[valid_df[pm25_base_col] < 60]["abs_error"].mean()
polluted_env = valid_df[valid_df[pm25_base_col] >= 60]["abs_error"].mean()
print(f"Moderate / Normal Periods (< 60 µg/m³)  : MAE = {clean_env:.2f} µg/m³ (N={len(valid_df[valid_df[pm25_base_col] < 60])})")
print(f"Spike / Hotspot Periods   (>= 60 µg/m³) : MAE = {polluted_env:.2f} µg/m³ (N={len(valid_df[valid_df[pm25_base_col] >= 60])})")
print("=" * 70)