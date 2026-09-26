"""
AeroSentinel - Master Phase 3 ML Intelligence Pipeline
File: ai-service/ml/pipeline.py

Executes:
  1. Ingestion of F2 chronological datasets
  2. Hotspot target construction
  3. Hotspot classification and probability calibration
  4. Multi-horizon PM2.5 forecast modeling
  5. Performance benchmarking against persistence baselines
  6. Confidence estimation and structured evidence generation
  7. Export of versioned model artifacts and evaluation ledger
"""

from pathlib import Path
import json
import joblib
import numpy as np
import pandas as pd

from app.utils.config import settings
from ml.hotspot.target import create_hotspot_target
from ml.hotspot.model import HotspotModelTrainer
from ml.forecast.model import ForecastModelTrainer
from ml.evaluation.metrics import evaluate_hotspot_classifier, evaluate_forecast_regressor
from ml.inference.confidence import compute_prediction_confidence
from ml.explainability.evidence import generate_structured_evidence


def run_f3_intelligence_pipeline() -> None:
    print("=" * 72)
    print("AERO-SENTINEL: Phase 3 (F3) AI/ML Intelligence Pipeline")
    print("=" * 72)

    processed_dir = settings.PROCESSED_DATA_DIR
    train_file = processed_dir / "train_dataset.parquet"
    val_file = processed_dir / "val_dataset.parquet"
    test_file = processed_dir / "test_dataset.parquet"

    # 1. Load F2 Chronological Parquet Partitions
    train_df = pd.read_parquet(train_file)
    val_df = pd.read_parquet(val_file)
    test_df = pd.read_parquet(test_file)

    print(f"Loaded F2 Partitions:")
    print(f"  Train: {train_df.shape[0]} rows | Val: {val_df.shape[0]} rows | Test: {test_df.shape[0]} rows")

    # 2. Derive Hotspot Targets
    train_df = create_hotspot_target(train_df)
    val_df = create_hotspot_target(val_df)
    test_df = create_hotspot_target(test_df)

    # 3. Define Input Feature Matrix (Excluding Metadata & Leakage Targets)
    excluded_cols = [
        "observation_id", "source", "source_record_id", "observed_at", "hourly_bin",
        "city_id", "station_id", "station_name", "h3_cell_id", "data_status",
        "quality_flag", "weather_quality_flag",
        "is_potential_hotspot",
        "target_pm25_t_plus_1", "target_pm25_t_plus_3", "target_pm25_t_plus_6"
    ]
    
    # Safe selection via Pandas numeric type detection
    all_num_cols = train_df.select_dtypes(include=[np.number]).columns.tolist()
    feature_cols = [c for c in all_num_cols if c not in excluded_cols]
    
    print(f"\nEngineered Features Used for Training ({len(feature_cols)} features):")
    print(f"  {feature_cols[:6]} ... {feature_cols[-4:]}")

    # =================================================================
    # 4. HOTSPOT CLASSIFICATION & CALIBRATION
    # =================================================================
    print("\n" + "-" * 72)
    print("1. TRAINING POTENTIAL HOTSPOT CLASSIFIER")
    print("-" * 72)

    hotspot_trainer = HotspotModelTrainer(feature_cols=feature_cols, random_state=42)
    hotspot_trainer.train_models(
        X_train=train_df,
        y_train=train_df["is_potential_hotspot"],
        X_val=val_df,
        y_val=val_df["is_potential_hotspot"]
    )

    y_test_hotspot = test_df["is_potential_hotspot"]
    hotspot_preds, hotspot_probs = hotspot_trainer.predict(test_df, model_name="random_forest")
    h_metrics = evaluate_hotspot_classifier(y_test_hotspot, hotspot_preds, hotspot_probs)

    print(f"Hotspot Detection Performance (Test Set):")
    print(f"  Precision: {h_metrics['precision']:.2f} | Recall: {h_metrics['recall']:.2f} | F1: {h_metrics['f1']:.2f}")
    print(f"  PR-AUC:    {h_metrics['pr_auc']:.2f} | ROC-AUC: {h_metrics['roc_auc']:.2f} | Brier: {h_metrics['brier_score']:.4f}")

    # =================================================================
    # 5. MULTI-HORIZON PM2.5 FORECASTING
    # =================================================================
    print("\n" + "-" * 72)
    print("2. TRAINING MULTI-HORIZON FORECAST REGRESSORS")
    print("-" * 72)

    forecast_trainer = ForecastModelTrainer(feature_cols=feature_cols, horizons=[1, 3, 6], random_state=42)
    forecast_trainer.train(train_df)

    forecast_results = {}
    for h in [1, 3, 6]:
        target_col = f"target_pm25_t_plus_{h}"
        # Evaluate on Test
        test_valid = test_df.dropna(subset=[target_col]).copy()
        if test_valid.empty:
            test_valid = test_df.copy()
            test_valid[target_col] = test_valid["pm25_clean"]

        y_true = test_valid[target_col]
        
        # Baselines
        pred_persist = forecast_trainer.predict_persistence(test_valid)
        pred_mean = forecast_trainer.predict_mean(test_valid, horizon=h)
        pred_rf = forecast_trainer.predict_model(test_valid, horizon=h)

        m_persist = evaluate_forecast_regressor(y_true, pred_persist)
        m_mean = evaluate_forecast_regressor(y_true, pred_mean)
        m_rf = evaluate_forecast_regressor(y_true, pred_rf)

        forecast_results[f"T+{h}"] = {"persistence": m_persist, "mean": m_mean, "random_forest": m_rf}

        print(f"Horizon T+{h} Forecast Benchmark:")
        print(f"  [Persistence]   MAE: {m_persist['mae']:6.2f} ug/m3 | RMSE: {m_persist['rmse']:6.2f} | R2: {m_persist['r2']}")
        print(f"  [Hist Mean]     MAE: {m_mean['mae']:6.2f} ug/m3 | RMSE: {m_mean['rmse']:6.2f} | R2: {m_mean['r2']}")
        print(f"  [Random Forest] MAE: {m_rf['mae']:6.2f} ug/m3 | RMSE: {m_rf['rmse']:6.2f} | R2: {m_rf['r2']}")

    # =================================================================
    # 6. EVIDENCE & CONFIDENCE SYNTHESIS (SAMPLE ROW)
    # =================================================================
    print("\n" + "-" * 72)
    print("3. SYNTHESIZING EXPLAINABLE EVENT INTELLIGENCE (TEST ROW 1)")
    print("-" * 72)

    sample_row = test_df.iloc[0]
    sample_conf = compute_prediction_confidence(sample_row)
    sample_evidence = generate_structured_evidence(sample_row, hotspot_score=float(hotspot_probs[0]))

    sample_event = {
        "timestamp": str(sample_row["observed_at"]),
        "h3_cell_id": str(sample_row["h3_cell_id"]),
        "station_id": str(sample_row["station_id"]),
        "hotspot": {
            "score": round(float(hotspot_probs[0]), 3),
            "is_potential_hotspot": bool(hotspot_preds[0]),
            "risk_level": "HIGH" if hotspot_probs[0] >= 0.70 else ("MODERATE" if hotspot_probs[0] >= 0.40 else "LOW"),
            "model_version": "hotspot_rf_v1.0"
        },
        "forecast": {
            "pm25_t_plus_1": round(float(forecast_trainer.predict_model(test_df.iloc[[0]], horizon=1).iloc[0]), 1),
            "unit": "ug/m3",
            "model_version": "forecast_rf_v1.0"
        },
        "confidence": sample_conf,
        "evidence_signals": sample_evidence
    }

    print(json.dumps(sample_event, indent=2))

    # =================================================================
    # 7. EXPORT VERSIONED ARTIFACTS
    # =================================================================
    model_dir = Path("models/artifacts")
    model_dir.mkdir(parents=True, exist_ok=True)

    joblib.dump(hotspot_trainer, model_dir / "hotspot_classifier_v1.joblib")
    joblib.dump(forecast_trainer, model_dir / "forecast_regressors_v1.joblib")

    with open(model_dir / "sample_event_output.json", "w", encoding="utf-8") as f:
        json.dump(sample_event, f, indent=2)

    print("\n" + "=" * 72)
    print("[F3 PIPELINE] AI/ML Intelligence Pipeline completed successfully.")
    print(f"Artifacts exported to: {model_dir.resolve()}")
    print("=" * 72)


if __name__ == "__main__":
    run_f3_intelligence_pipeline()