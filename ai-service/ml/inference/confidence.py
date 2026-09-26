"""
AeroSentinel - Prediction Confidence & Uncertainty Estimation
File: ai-service/ml/inference/confidence.py

Calculates prediction confidence considering:
  - Sensor data completeness & missingness
  - Monitoring coverage gap flags (epistemic uncertainty)
  - Distance to nearest active monitoring station
  - Model calibration certainty near decision boundaries
  - Empirical multi-horizon forecast prediction intervals (P10 - P90)
"""

from typing import Dict, Any, Union, Optional
import numpy as np
import pandas as pd

# Empirical holdout test set residual bounds (T+1h, T+3h, T+6h)
PREDICTION_INTERVAL_BOUNDS = {
    1: {"q10": -2.41, "q90": 2.15},
    3: {"q10": -5.12, "q90": 4.88},
    6: {"q10": -9.34, "q90": 8.76}
}


def compute_prediction_confidence(
    row: Union[pd.Series, Dict[str, Any]],
    predicted_probability: Optional[float] = None
) -> Dict[str, Any]:
    """
    Computes calibrated confidence factors for an individual observation or H3 cell.
    
    Guiding Principle:
      Low monitoring coverage increases UNCERTAINTY, not pollution risk.
      Confidence is NOT model probability.
    """
    data = row if isinstance(row, dict) else row.to_dict()

    # 1. Data Quality & Completeness Factor [0.0 - 1.0]
    core_pollutants = ["pm25_clean", "pm25", "pm10", "no2", "temperature", "humidity"]
    present_count = sum(
        1 for p in core_pollutants 
        if p in data and data[p] is not None and not (isinstance(data[p], float) and np.isnan(data[p]))
    )
    # Scaled to core 5 features (pm25/pm25_clean count as primary target indicator)
    data_quality_factor = round(min(1.0, present_count / 5.0), 3)

    # 2. Monitoring Coverage Confidence [0.0 - 1.0]
    # Distant cells or unmonitored blindspots heavily penalize coverage certainty
    nearest_dist = float(data.get("nearest_station_distance_km", 0.0) or 0.0)
    is_gap = int(data.get("monitoring_coverage_gap_flag", 0) or 0)
    
    if is_gap == 1 or nearest_dist > 7.0:
        coverage_confidence = max(0.15, round(np.exp(-max(0.0, nearest_dist - 5.0) / 25.0), 3))
    else:
        coverage_confidence = 0.95

    # 3. Model Calibration Certainty [0.0 - 1.0]
    # Penalizes ambiguity when probability hovers directly on the decision boundary (0.40 - 0.60)
    if predicted_probability is not None:
        boundary_distance = abs(predicted_probability - 0.50)
        model_certainty = round(float(np.clip(0.50 + boundary_distance, 0.50, 1.0)), 3)
    else:
        model_certainty = 0.90 if data_quality_factor >= 0.80 else 0.65

    # 4. Multi-Factor Composite Confidence Score
    # Safe weighted combination avoiding zero-division
    overall = round(
        (0.35 * data_quality_factor) + 
        (0.40 * coverage_confidence) + 
        (0.25 * model_certainty),
        3
    )

    # Epistemic uncertainty is flagged when spatial coverage is weak or inputs are severely missing
    epistemic_uncertainty_flag = 1 if (is_gap == 1 or nearest_dist > 15.0 or data_quality_factor < 0.40) else 0

    return {
        "overall_confidence": overall,
        "data_quality_score": data_quality_factor,
        "coverage_confidence": coverage_confidence,
        "model_certainty": model_certainty,
        "nearest_station_distance_km": round(nearest_dist, 2),
        "epistemic_uncertainty_flag": epistemic_uncertainty_flag
    }


def compute_forecast_intervals(
    point_forecasts: Dict[str, float]
) -> Dict[str, Dict[str, float]]:
    """
    Computes empirical P10-P90 prediction intervals for multi-horizon forecasts.
    Protects against negative PM2.5 physical violations.
    """
    intervals = {}
    for horizon in [1, 3, 6]:
        h_key = f"T+{horizon}h"
        if h_key in point_forecasts:
            y_hat = float(point_forecasts[h_key])
            q10 = PREDICTION_INTERVAL_BOUNDS[horizon]["q10"]
            q90 = PREDICTION_INTERVAL_BOUNDS[horizon]["q90"]
            intervals[h_key] = {
                "point_estimate": round(y_hat, 2),
                "lower_p10": round(max(0.0, y_hat + q10), 2),
                "upper_p90": round(y_hat + q90, 2)
            }
    return intervals