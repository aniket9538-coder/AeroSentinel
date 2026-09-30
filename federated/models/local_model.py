"""
AeroSentinel - Federated Local Model Engine
File: federated/models/local_model.py

Standardized local hotspot classification model wrapping scikit-learn Logistic Regression
with StandardScaler. Enforces the canonical 36-feature schema (f3-features-v1) and enables
clean parameter weight extraction, parameter injection, evaluation, and update serialization.
"""

from typing import Dict, List, Optional, Tuple, Any
import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (
    mean_absolute_error,
    mean_squared_error,
    roc_auc_score,
    brier_score_loss,
    accuracy_score,
)

FEATURE_SCHEMA_VERSION = "f3-features-v1"
FEATURE_COUNT = 36

ORDERED_FEATURE_NAMES: List[str] = [
    "latitude",
    "longitude",
    "pm10",
    "no2",
    "so2",
    "co",
    "o3",
    "hour",
    "day_of_week",
    "is_weekend",
    "hour_sin",
    "hour_cos",
    "dow_sin",
    "dow_cos",
    "temperature",
    "humidity",
    "wind_speed",
    "wind_direction",
    "wind_u",
    "wind_v",
    "rainfall",
    "pressure",
    "pm25_spatial_lag_mean",
    "nearest_station_distance_km",
    "stations_within_5km_count",
    "monitoring_coverage_gap_flag",
    "dist_to_nearest_industrial_km",
    "dist_to_nearest_major_road_km",
    "sensitive_receptors_count_2km",
    "industrial_zone_within_2km_flag",
    "fire_count_24h_25km",
    "fire_frp_sum_24h_25km",
    "fire_frp_mean_24h_25km",
    "nearest_fire_distance_km",
    "fire_frp_distance_decay",
    "fire_upwind_alignment_score",
]


class LocalHotspotModel:
    """
    Local municipal model for hotspot risk probability estimation.
    Enforces standardized 36-feature vector representation.
    """

    def __init__(self, random_state: int = 42):
        self.random_state = random_state
        self.scaler = StandardScaler()
        self.classifier = LogisticRegression(
            class_weight="balanced",
            max_iter=2000,
            solver="lbfgs",
            random_state=self.random_state,
        )
        self.is_fitted = False
        self.feature_names = ORDERED_FEATURE_NAMES.copy()

    def _prepare_features(self, X: pd.DataFrame) -> np.ndarray:
        """Validates and extracts the ordered 36-feature matrix."""
        missing = [f for f in self.feature_names if f not in X.columns]
        if missing:
            raise ValueError(f"Input DataFrame is missing required features: {missing}")

        X_ordered = X[self.feature_names].fillna(0.0).astype(float)
        return X_ordered.values

    def fit(self, X: pd.DataFrame, y: pd.Series) -> "LocalHotspotModel":
        """
        Fits StandardScaler and Logistic Regression on local 36-feature dataset.
        """
        X_mat = self._prepare_features(X)
        y_vec = np.asarray(y, dtype=int)

        if len(np.unique(y_vec)) < 2:
            # Handle single-class edge case by ensuring dummy binary representation
            y_vec = np.copy(y_vec)
            y_vec[0] = 1 - y_vec[0]

        X_scaled = self.scaler.fit_transform(X_mat)
        self.classifier.fit(X_scaled, y_vec)
        self.is_fitted = True
        return self

    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """Returns risk probabilities for class 1 (hotspot indicator)."""
        if not self.is_fitted:
            raise RuntimeError("Model must be fitted before predict_proba() can be called.")
        X_mat = self._prepare_features(X)
        X_scaled = self.scaler.transform(X_mat)
        probs = self.classifier.predict_proba(X_scaled)
        if probs.shape[1] == 2:
            return probs[:, 1]
        return probs[:, 0]

    def predict(self, X: pd.DataFrame, threshold: float = 0.5) -> np.ndarray:
        """Returns binary predictions using configurable operational threshold."""
        probs = self.predict_proba(X)
        return (probs >= threshold).astype(int)

    def evaluate(self, X: pd.DataFrame, y: pd.Series) -> Dict[str, float]:
        """
        Computes standard evaluation metrics: MAE, RMSE, ROC-AUC, Brier Score, Accuracy.
        """
        if not self.is_fitted:
            raise RuntimeError("Model must be fitted before evaluation.")

        y_true = np.asarray(y, dtype=float)
        y_prob = self.predict_proba(X)
        y_pred = (y_prob >= 0.5).astype(int)

        mae = float(mean_absolute_error(y_true, y_prob))
        rmse = float(np.sqrt(mean_squared_error(y_true, y_prob)))
        brier = float(brier_score_loss(y_true, y_prob))
        acc = float(accuracy_score(y_true, y_pred))

        # Handle ROC-AUC edge cases
        if len(np.unique(y_true)) > 1:
            roc_auc = float(roc_auc_score(y_true, y_prob))
        else:
            roc_auc = 0.5

        return {
            "mae": round(mae, 4),
            "rmse": round(rmse, 4),
            "rocAuc": round(roc_auc, 4),
            "brierScore": round(brier, 4),
            "accuracy": round(acc, 4),
        }

    def get_weights(self) -> Tuple[List[float], float]:
        """
        Returns (weights, intercept) where weights is a 36-element float list.
        """
        if not self.is_fitted:
            raise RuntimeError("Cannot extract weights from unfitted model.")

        raw_weights = self.classifier.coef_[0].tolist()
        raw_intercept = float(self.classifier.intercept_[0])

        if len(raw_weights) != FEATURE_COUNT:
            raise ValueError(f"Weight vector length {len(raw_weights)} != {FEATURE_COUNT}")

        # Check for NaNs or Inf
        if np.isnan(raw_weights).any() or np.isinf(raw_weights).any():
            raise ValueError("Model weights contain NaN or Inf values.")

        return [round(float(w), 6) for w in raw_weights], round(raw_intercept, 6)

    def set_weights(self, weights: List[float], intercept: float) -> None:
        """
        Injects global or updated model weights and intercept into the classifier.
        """
        if len(weights) != FEATURE_COUNT:
            raise ValueError(f"Expected {FEATURE_COUNT} weights, received {len(weights)}")

        weights_arr = np.asarray(weights, dtype=float)
        if np.isnan(weights_arr).any() or np.isinf(weights_arr).any():
            raise ValueError("Input weights contain NaN or Inf values.")

        self.classifier.coef_ = np.array([weights_arr])
        self.classifier.intercept_ = np.array([float(intercept)])
        self.classifier.classes_ = np.array([0, 1])

        # If scaler hasn't been fitted yet, initialize with neutral identity scaling
        if not hasattr(self.scaler, "mean_") or self.scaler.mean_ is None:
            self.scaler.mean_ = np.zeros(FEATURE_COUNT)
            self.scaler.scale_ = np.ones(FEATURE_COUNT)
            self.scaler.var_ = np.ones(FEATURE_COUNT)
            self.scaler.n_samples_seen_ = 1000

        self.is_fitted = True

    def save(self, file_path: str) -> None:
        """Persists the model artifact (weights + scaler)."""
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        joblib.dump({
            "schema_version": FEATURE_SCHEMA_VERSION,
            "feature_names": self.feature_names,
            "scaler": self.scaler,
            "classifier": self.classifier,
            "is_fitted": self.is_fitted,
        }, file_path)

    @classmethod
    def load(cls, file_path: str) -> "LocalHotspotModel":
        """Loads a persisted model artifact."""
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Model artifact not found at: {file_path}")

        data = joblib.load(file_path)
        instance = cls()
        instance.scaler = data["scaler"]
        instance.classifier = data["classifier"]
        instance.feature_names = data.get("feature_names", ORDERED_FEATURE_NAMES.copy())
        instance.is_fitted = data.get("is_fitted", True)
        return instance
