"""
AeroSentinel - Production Model Training Pipeline (F3)
File: ai-service/ml/training/train_all_models.py
Fulfills Section 9 (Candidate Hierarchy), Section 19 (Baseline Comparisons), 
Section 36 (Reproducibility), and Section 41 (Test Data Protection).
"""

from pathlib import Path
import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.calibration import CalibratedClassifierCV

from ml.evaluation.evaluator import evaluate_hotspot_classifier, evaluate_forecaster_horizon


def load_datasets():
    """Load chronologically partitioned training, validation, and untouched test parquet datasets."""
    module_dir = Path(__file__).resolve().parent
    repo_root = None
    for parent in [module_dir, *module_dir.parents]:
        if (parent / "data" / "processed" / "train_dataset.parquet").exists():
            repo_root = parent
            break

    if repo_root is not None:
        train_path = repo_root / "data" / "processed" / "train_dataset.parquet"
        val_path = repo_root / "data" / "processed" / "val_dataset.parquet"
        test_path = repo_root / "data" / "processed" / "test_dataset.parquet"
    else:
        repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
        train_path = repo_root / "data" / "processed" / "train_dataset.parquet"
        val_path = repo_root / "data" / "processed" / "val_dataset.parquet"
        test_path = repo_root / "data" / "processed" / "test_dataset.parquet"

    train_df = pd.read_parquet(train_path)
    val_df = pd.read_parquet(val_path)
    test_df = pd.read_parquet(test_path)
    return train_df, val_df, test_df


def select_features(df: pd.DataFrame):
    """Exclude non-feature identifiers, target columns, and unpopulated columns to avoid leakage."""
    exclude = [
        "observed_at", "hourly_bin", "station_id", "city_id", "h3_cell_id",
        "target_pm25_t_plus_1", "target_pm25_t_plus_3", "target_pm25_t_plus_6",
        "pm25", "pm25_clean", "pm25_raw",
        "satellite_aerosol_index", "satellite_co_column", "satellite_so2_column",
        "satellite_cloud_fraction", "satellite_no2_trop"
    ]
    candidate_df = df.drop(columns=[c for c in exclude if c in df.columns])
    numeric_df = candidate_df.select_dtypes(include=[np.number])
    return list(numeric_df.columns)


def train_and_benchmark_hotspots(train_df: pd.DataFrame, val_df: pd.DataFrame, test_df: pd.DataFrame, features: list):
    """
    Evaluates Hotspot Hierarchy:
      1. Baseline: Logistic Regression (class_weight='balanced')
      2. Tree Model: Balanced Random Forest
      3. Calibrated Production Model: Platt-Calibrated RF (cv=3)
    Evaluates final performance against the untouched Test Set.
    """
    target_col = "pm25_clean" if "pm25_clean" in train_df.columns else "pm25"
    y_train = (train_df[target_col].fillna(0.0) >= 60.0).astype(int)
    y_val = (val_df[target_col].fillna(0.0) >= 60.0).astype(int)
    y_test = (test_df[target_col].fillna(0.0) >= 60.0).astype(int)

    X_train = train_df[features].fillna(0.0)
    X_val = val_df[features].fillna(0.0)
    X_test = test_df[features].fillna(0.0)

    # 1. Baseline Model: Logistic Regression
    print("Fitting Baseline 1: Logistic Regression (balanced)...")
    lr = LogisticRegression(class_weight="balanced", max_iter=500, random_state=42)
    lr.fit(X_train, y_train)
    lr_val_prob = lr.predict_proba(X_val)[:, 1]
    lr_metrics = evaluate_hotspot_classifier(y_val.values, lr_val_prob, threshold=0.50)
    print(f"  • Logistic Regression Val -> F1: {lr_metrics['f1']:.3f} | PR-AUC: {lr_metrics['pr_auc']:.3f}")

    # 2. Tree Model: Balanced Random Forest
    print("Fitting Tree Model 2: Balanced Random Forest...")
    rf_base = RandomForestClassifier(
        n_estimators=100,
        max_depth=12,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1
    )
    rf_base.fit(X_train, y_train)
    rf_val_prob = rf_base.predict_proba(X_val)[:, 1]
    rf_metrics = evaluate_hotspot_classifier(y_val.values, rf_val_prob, threshold=0.50)
    print(f"  • Balanced RF Val -> F1: {rf_metrics['f1']:.3f} | PR-AUC: {rf_metrics['pr_auc']:.3f}")

    # 3. Production Model: Platt-Calibrated RF
    print("Fitting Production Model 3: Platt Sigmoid Calibration (cv=3) over Balanced RF...")
    calibrated_rf = CalibratedClassifierCV(estimator=rf_base, method="sigmoid", cv=3)
    calibrated_rf.fit(X_train, y_train)
    cal_val_prob = calibrated_rf.predict_proba(X_val)[:, 1]
    cal_metrics = evaluate_hotspot_classifier(y_val.values, cal_val_prob, threshold=0.20)
    print(f"  • Calibrated RF Val (threshold=0.20) -> F1: {cal_metrics['f1']:.3f} | Brier: {cal_metrics['brier_score']:.4f}")

    # Final Verification on Untouched Test Set
    cal_test_prob = calibrated_rf.predict_proba(X_test)[:, 1]
    test_metrics = evaluate_hotspot_classifier(y_test.values, cal_test_prob, threshold=0.20)
    print(f"  >>> TEST SET EVALUATION -> F1: {test_metrics['f1']:.3f} | PR-AUC: {test_metrics['pr_auc']:.3f} | ROC-AUC: {test_metrics['roc_auc']:.3f}")

    return {
        "model": calibrated_rf,
        "feature_cols": features,
        "operational_threshold": 0.20,
        "algorithm": "CalibratedClassifierCV(Sigmoid, cv=3) over Balanced RandomForest",
        "benchmark_summary": {
            "logistic_regression": lr_metrics,
            "balanced_rf": rf_metrics,
            "calibrated_rf_val": cal_metrics,
            "calibrated_rf_test": test_metrics
        }
    }


def train_and_benchmark_forecasters(train_df: pd.DataFrame, val_df: pd.DataFrame, test_df: pd.DataFrame, features: list):
    """
    Trains multi-horizon regressors (T+1h, T+3h, T+6h) and benchmarks each against
    the Naive Persistence baseline on both Validation and untouched Test partitions.
    """
    current_col = "pm25_clean" if "pm25_clean" in train_df.columns else "pm25"
    forecaster_models = {}
    residuals = {}
    benchmark_metrics = {}

    for h in [1, 3, 6]:
        t_col = f"target_pm25_t_plus_{h}"
        valid_train = train_df.dropna(subset=[t_col, current_col])
        valid_val = val_df.dropna(subset=[t_col, current_col])
        valid_test = test_df.dropna(subset=[t_col, current_col])

        print(f"Training Regressor for Horizon T+{h}h...")
        rf_reg = RandomForestRegressor(
            n_estimators=100,
            max_depth=14,
            random_state=42,
            n_jobs=-1
        )
        rf_reg.fit(valid_train[features].fillna(0.0), valid_train[t_col])
        forecaster_models[h] = rf_reg
        forecaster_models[f"t_plus_{h}"] = rf_reg

        # Val evaluation vs Naive persistence
        val_preds = rf_reg.predict(valid_val[features].fillna(0.0))
        val_metrics = evaluate_forecaster_horizon(
            valid_val[t_col].values,
            val_preds,
            valid_val[current_col].values
        )
        print(f"  • Horizon T+{h}h Val MAE: {val_metrics['mae']:.2f} (Naive: {val_metrics['naive_mae']:.2f}, R2: {val_metrics['r2']:.4f})")

        # Test evaluation vs Naive persistence
        test_preds = rf_reg.predict(valid_test[features].fillna(0.0))
        test_metrics = evaluate_forecaster_horizon(
            valid_test[t_col].values,
            test_preds,
            valid_test[current_col].values
        )
        print(f"  >>> Horizon T+{h}h Test MAE: {test_metrics['mae']:.2f} (Naive: {test_metrics['naive_mae']:.2f}, R2: {test_metrics['r2']:.4f})")

        benchmark_metrics[f"T+{h}h"] = {
            "val": val_metrics,
            "test": test_metrics
        }

        # Calculate empirical validation residual intervals
        val_res = valid_val[t_col].values - val_preds
        residuals[h] = {
            "p10": float(np.percentile(val_res, 10)),
            "p90": float(np.percentile(val_res, 90))
        }

    return {
        "models": forecaster_models,
        "feature_cols": features,
        "residuals": residuals,
        "benchmark_metrics": benchmark_metrics,
        "algorithm": "RandomForestRegressor(n_estimators=100, max_depth=14)"
    }


def main():
    print("=" * 70)
    print("AeroSentinel F3 AI/ML Production Training & Benchmarking Pipeline")
    print("=" * 70)

    train_df, val_df, test_df = load_datasets()
    print(f"Dataset Partitions Loaded:")
    print(f"  • Train: {len(train_df):,} rows")
    print(f"  • Val:   {len(val_df):,} rows")
    print(f"  • Test:  {len(test_df):,} rows (untouched holdout)")

    features = select_features(train_df)
    print(f"Selected {len(features)} numeric input features (leakage-free).")

    print("\n--- Benchmarking & Training Hotspot Models ---")
    hotspot_artifact = train_and_benchmark_hotspots(train_df, val_df, test_df, features)

    print("\n--- Benchmarking & Training Forecasters ---")
    forecast_artifact = train_and_benchmark_forecasters(train_df, val_df, test_df, features)

    # Resolve artifacts directory relative to repository root
    repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
    artifacts_dir = repo_root / "ai-service" / "models" / "artifacts"
    artifacts_dir.mkdir(parents=True, exist_ok=True)

    joblib.dump(hotspot_artifact, artifacts_dir / "hotspot_classifier_v1.joblib")
    joblib.dump(forecast_artifact, artifacts_dir / "forecast_regressors_v1.joblib")

    print("\n" + "=" * 70)
    print(f"Artifacts successfully saved to: {artifacts_dir}")
    print(" - hotspot_classifier_v1.joblib")
    print(" - forecast_regressors_v1.joblib")
    print("=" * 70)


if __name__ == "__main__":
    main()