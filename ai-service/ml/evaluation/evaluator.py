"""
AeroSentinel - Reusable Model Evaluation & Confidence Estimation Engine (F3)
File: ai-service/ml/evaluation/evaluator.py
Fulfills Section 22 (Metrics), Section 24 (Confidence), Section 27 (Coverage Uncertainty).
"""

from typing import Dict, Any, List, Optional
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
    mean_squared_error,
    r2_score,
)


def evaluate_hotspot_classifier(
    y_true: np.ndarray,
    y_prob: np.ndarray,
    threshold: float = 0.20
) -> Dict[str, float]:
    """Calculate comprehensive evaluation metrics for calibrated hotspot classifiers."""
    y_pred = (y_prob >= threshold).astype(int)
    
    metrics = {
        "threshold": float(threshold),
        "precision": float(precision_score(y_true, y_pred, zero_division=0)),
        "recall": float(recall_score(y_true, y_pred, zero_division=0)),
        "f1": float(f1_score(y_true, y_pred, zero_division=0)),
        "brier_score": float(brier_score_loss(y_true, y_prob)),
    }
    
    # Calculate ranking metrics if both classes exist in y_true
    if len(np.unique(y_true)) > 1:
        metrics["roc_auc"] = float(roc_auc_score(y_true, y_prob))
        metrics["pr_auc"] = float(average_precision_score(y_true, y_prob))
    else:
        metrics["roc_auc"] = 0.0
        metrics["pr_auc"] = 0.0
        
    return metrics


def evaluate_forecaster_horizon(
    y_true: np.ndarray,
    y_pred: np.ndarray,
    y_naive: Optional[np.ndarray] = None
) -> Dict[str, float]:
    """Compute regression metrics and compare against persistence baseline."""
    mask = ~np.isnan(y_true) & ~np.isnan(y_pred)
    yt = y_true[mask]
    yp = y_pred[mask]
    
    mae = float(mean_absolute_error(yt, yp))
    rmse = float(np.sqrt(mean_squared_error(yt, yp)))
    r2 = float(r2_score(yt, yp))
    
    result = {
        "mae": round(mae, 2),
        "rmse": round(rmse, 2),
        "r2": round(r2, 4),
        "sample_count": int(len(yt))
    }
    
    if y_naive is not None:
        yn = y_naive[mask]
        result["naive_mae"] = round(float(mean_absolute_error(yt, yn)), 2)
        result["naive_rmse"] = round(float(np.sqrt(mean_squared_error(yt, yn))), 2)
        result["naive_r2"] = round(float(r2_score(yt, yn)), 4)
        result["mae_improvement_pct"] = round(float((result["naive_mae"] - mae) / max(result["naive_mae"], 1e-4) * 100), 2)
        
    return result


def compute_hyperlocal_confidence(
    calibrated_prob: float,
    operational_threshold: float,
    feature_completeness: float,
    nearest_station_distance_km: float,
    monitoring_coverage_gap_flag: int = 0
) -> Dict[str, float]:
    """
    Composite Confidence Scoring (Section 24, 27).
    Fuses:
      1. Boundary margin: Confidence decays near the ambiguous 0.20 threshold.
      2. Feature completeness: Ratio of non-null required predictors [0, 1].
      3. Coverage uncertainty: Spatial distance to nearest continuous ground monitor.
    Does NOT infer pollution risk from missing coverage.
    """
    # 1. Model prediction margin confidence
    margin = abs(calibrated_prob - operational_threshold)
    max_margin = max(operational_threshold, 1.0 - operational_threshold)
    model_conf = np.clip(margin / max_margin, 0.10, 1.0)
    
    # 2. Coverage penalty (penalize if nearest station > 10 km or gap flag raised)
    dist_penalty = np.clip(1.0 - (nearest_station_distance_km / 25.0), 0.20, 1.0)
    if monitoring_coverage_gap_flag == 1:
        dist_penalty *= 0.80

    # 3. Completeness score
    comp_score = np.clip(feature_completeness, 0.10, 1.0)
    
    # Weighted harmonic fusion
    overall_confidence = float(np.clip(
        0.50 * model_conf + 0.30 * comp_score + 0.20 * dist_penalty,
        0.05,
        1.0
    ))
    
    return {
        "overall_confidence": round(overall_confidence, 3),
        "model_margin_confidence": round(float(model_conf), 3),
        "data_completeness_score": round(float(comp_score), 3),
        "spatial_coverage_confidence": round(float(dist_penalty), 3)
    }