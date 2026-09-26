"""
AeroSentinel - Phase 3 Model Accuracy & Performance Audit
File: ai-service/models/evaluate_accuracy.py
"""

from pathlib import Path
import joblib
import pandas as pd
from app.utils.config import settings
from ml.evaluation.metrics import evaluate_hotspot_classifier, evaluate_forecast_regressor
from ml.hotspot.target import create_hotspot_target


def show_accuracy():
    # 1. Load test dataset and trained artifacts
    test_path = settings.PROCESSED_DATA_DIR / "test_dataset.parquet"
    if not test_path.exists():
        print(f"Error: {test_path} not found. Run the pipeline first.")
        return

    test_df = create_hotspot_target(pd.read_parquet(test_path))
    hotspot_trainer = joblib.load("models/artifacts/hotspot_classifier_v1.joblib")
    forecast_trainer = joblib.load("models/artifacts/forecast_regressors_v1.joblib")

    print("=" * 65)
    print("AEROSENTINEL — PHASE 3 MODEL ACCURACY & PERFORMANCE AUDIT")
    print("=" * 65)

    # 2. Hotspot Classifier Accuracy
    preds, probs = hotspot_trainer.predict(test_df)
    h_metrics = evaluate_hotspot_classifier(test_df["is_potential_hotspot"], preds, probs)
    acc = (preds == test_df["is_potential_hotspot"]).mean() * 100.0

    print("\n[1] POTENTIAL HOTSPOT CLASSIFIER ACCURACY:")
    print(f"  -> Accuracy:           {acc:.1f}%")
    print(f"  -> Precision:          {h_metrics['precision']:.2f}")
    print(f"  -> Recall:             {h_metrics['recall']:.2f}")
    print(f"  -> F1-Score:           {h_metrics['f1']:.2f}")
    print(f"  -> Brier Score (Loss): {h_metrics['brier_score']:.4f}")

    # 3. Forecast Regressor Accuracy (T+1 Horizon)
    y_true = test_df["target_pm25_t_plus_1"].fillna(test_df["pm25_clean"])
    y_pred = forecast_trainer.predict_model(test_df, horizon=1)
    f_metrics = evaluate_forecast_regressor(y_true, y_pred)

    print("\n[2] PM2.5 FORECAST ACCURACY (T+1 Horizon):")
    print(f"  -> Mean Absolute Error (MAE):     {f_metrics['mae']} ug/m3")
    print(f"  -> Root Mean Squared Error (RMSE): {f_metrics['rmse']} ug/m3")
    print(f"  -> R-Squared (R2) Variance Score: {f_metrics['r2']}")

    # 4. Row-by-Row Test Set Inferences
    print("\n[3] ROW-BY-ROW TEST PREDICTIONS:")
    for i in range(len(test_df)):
        risk_str = "HIGH" if probs[i] >= 0.70 else ("MODERATE" if probs[i] >= 0.40 else "LOW")
        print(
            f"  Row {i+1} | "
            f"Actual PM2.5: {y_true.iloc[i]:5.1f} | "
            f"Forecast T+1: {y_pred.iloc[i]:5.1f} | "
            f"Hotspot Prob: {probs[i]:.2f} (Risk: {risk_str})"
        )
    print("=" * 65)


if __name__ == "__main__":
    show_accuracy()