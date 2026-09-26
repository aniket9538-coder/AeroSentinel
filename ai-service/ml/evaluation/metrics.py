"""
AeroSentinel - AI/ML Evaluation Metrics Suite
File: ai-service/ml/evaluation/metrics.py

Computes precision, recall, F1, PR-AUC, ROC-AUC, Brier score,
MAE, RMSE, R2, and horizon-specific performance comparisons.
"""

from typing import Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    roc_auc_score,
    average_precision_score,
    brier_score_loss,
    mean_absolute_error,
    root_mean_squared_error,
    r2_score,
)


def evaluate_hotspot_classifier(
    y_true: pd.Series,
    y_pred: np.ndarray,
    y_prob: Optional[np.ndarray] = None
) -> Dict[str, float]:
    """Calculates classification metrics robust to class imbalance."""
    metrics = {
        "precision": float(precision_score(y_true, y_pred, zero_division=0)),
        "recall": float(recall_score(y_true, y_pred, zero_division=0)),
        "f1": float(f1_score(y_true, y_pred, zero_division=0)),
    }
    if y_prob is not None and len(np.unique(y_true)) > 1:
        metrics["roc_auc"] = float(roc_auc_score(y_true, y_prob))
        metrics["pr_auc"] = float(average_precision_score(y_true, y_prob))
        metrics["brier_score"] = float(brier_score_loss(y_true, y_prob))
    else:
        metrics["roc_auc"] = 0.5
        metrics["pr_auc"] = float(y_true.mean()) if len(y_true) > 0 else 0.0
        metrics["brier_score"] = 0.0

    return metrics


def evaluate_forecast_regressor(
    y_true: pd.Series,
    y_pred: pd.Series
) -> Dict[str, float]:
    """Calculates regression forecasting accuracy."""
    mask = ~y_true.isna() & ~y_pred.isna()
    if mask.sum() == 0:
        return {"mae": 0.0, "rmse": 0.0, "r2": 0.0}

    yt = y_true[mask]
    yp = y_pred[mask]

    mae = float(mean_absolute_error(yt, yp))
    rmse = float(root_mean_squared_error(yt, yp))
    r2 = float(r2_score(yt, yp)) if len(yt) > 1 else 1.0

    return {
        "mae": round(mae, 2),
        "rmse": round(rmse, 2),
        "r2": round(r2, 3)
    }