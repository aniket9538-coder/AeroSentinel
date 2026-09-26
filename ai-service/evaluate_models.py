"""
AeroSentinel - Model Accuracy & Performance Evaluation
File: ai-service/evaluate_models.py
"""

from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import (
    classification_report,
    roc_auc_score,
    brier_score_loss,
    mean_squared_error,
    mean_absolute_error,
    r2_score,
)

# 1. Locate test dataset
candidate_paths = [
    Path("data/processed/test_dataset.parquet"),
    Path("../data/processed/test_dataset.parquet"),
    Path("test_dataset.parquet"),
]

test_path = next((p for p in candidate_paths if p.exists()), None)
if not test_path:
    found = list(Path(".").rglob("test_dataset.parquet")) + list(Path("..").rglob("test_dataset.parquet"))
    if found:
        test_path = found[0]
    else:
        raise FileNotFoundError("test_dataset.parquet not found. Run 'python -m preprocessing.pipeline' first.")

print(f"Loading test set from: {test_path.resolve()}")
test_df = pd.read_parquet(test_path)
print(f"Total holdout test samples: {len(test_df)}")

# 2. Evaluate Hotspot Classifier
hotspot_file = Path("models/artifacts/hotspot_classifier_v1.joblib")
feature_cols = None

if hotspot_file.exists():
    print("\n" + "=" * 62)
    print("1. EVALUATING HOTSPOT CLASSIFIER")
    print("=" * 62)
    hotspot_obj = joblib.load(hotspot_file)

    trainer = hotspot_obj.get("trainer", hotspot_obj) if isinstance(hotspot_obj, dict) else hotspot_obj
    feature_cols = getattr(trainer, "feature_cols", None)

    if feature_cols is None and isinstance(hotspot_obj, dict):
        feature_cols = hotspot_obj.get("features")

    if feature_cols is None:
        exclude_cols = [
            "observed_at", "hourly_bin", "station_id", "city_id",
            "h3_cell_id", "target_pm25_t_plus_1", "target_pm25_t_plus_3",
            "target_pm25_t_plus_6"
        ]
        feature_cols = [c for c in test_df.columns if c not in exclude_cols]

    target_series = test_df["pm25_clean"] if "pm25_clean" in test_df.columns else test_df.get("pm25")
    if target_series is not None:
        y_test = (target_series >= 60.0).astype(int)
        X_test = test_df[feature_cols].fillna(0.0)

        preds, probs = trainer.predict(X_test)

        print("\nStandard Evaluation (Default 0.50 Cutoff):")
        print(classification_report(y_test, preds, zero_division=0))

        if len(np.unique(y_test)) > 1:
            print(f"ROC-AUC Score:      {roc_auc_score(y_test, probs):.4f}")
            print(f"Brier Loss (Cal):   {brier_score_loss(y_test, probs):.4f}")

        print("\nOperational Threshold Comparison (Spike Sensitivity):")
        print("Threshold | Hotspot Recall | Hotspot Precision | F1-Score")
        print("-" * 55)
        for thresh in [0.05, 0.10, 0.15, 0.20, 0.25, 0.30]:
            t_preds = (probs >= thresh).astype(int)
            rep = classification_report(y_test, t_preds, output_dict=True, zero_division=0)
            c1 = rep["1"]
            print(f"   {thresh:.2f}   |     {c1['recall']*100:5.1f}%     |      {c1['precision']*100:5.1f}%      |   {c1['f1-score']:.3f}")
    else:
        print("No target column found to evaluate.")

# 3. Evaluate Multi-Horizon Regressors
forecast_file = Path("models/artifacts/forecast_regressors_v1.joblib")
if forecast_file.exists():
    print("\n" + "=" * 62)
    print("2. EVALUATING MULTI-HORIZON PM2.5 FORECASTERS")
    print("=" * 62)
    forecast_artifact = joblib.load(forecast_file)

    if isinstance(forecast_artifact, dict):
        models = forecast_artifact.get("models", forecast_artifact)
        features = forecast_artifact.get("features", feature_cols)
    else:
        models = getattr(forecast_artifact, "models", forecast_artifact)
        features = getattr(forecast_artifact, "feature_cols", feature_cols)

    # Clean forecast features list to avoid passing target or metadata columns
    clean_features = [f for f in features if f in test_df.columns and not f.startswith("target_")]

    for h in [1, 3, 6]:
        target_col = f"target_pm25_t_plus_{h}"
        
        # Check both integer key and string key
        model = None
        if isinstance(models, dict):
            if h in models:
                model = models[h]
            elif str(h) in models:
                model = models[str(h)]
            elif f"t_plus_{h}" in models:
                model = models[f"t_plus_{h}"]

        if target_col in test_df.columns and model is not None:
            valid_df = test_df.dropna(subset=[target_col])

            if len(valid_df) > 0:
                y_true = valid_df[target_col]
                y_pred = model.predict(valid_df[clean_features].fillna(0.0))

                mae = mean_absolute_error(y_true, y_pred)
                rmse = np.sqrt(mean_squared_error(y_true, y_pred))
                r2 = r2_score(y_true, y_pred)

                print(f"Horizon T+{h}h (Forecast +{h}h Ahead):")
                print(f"  • MAE (Mean Absolute Error):  {mae:.2f} µg/m³")
                print(f"  • RMSE (Root Mean Sq Error):  {rmse:.2f} µg/m³")
                print(f"  • R² Score (Fit Quality):     {r2:.4f}")
                print("-" * 42)