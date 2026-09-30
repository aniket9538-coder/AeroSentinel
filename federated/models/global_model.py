"""
AeroSentinel - Federated Global Model Representation
File: federated/models/global_model.py

Represents the centralized consensus model produced by coordinator aggregation.
Maintains 36-feature weight parameters, global intercept, version lineage,
and evaluation benchmarks for inference and distribution back to municipal nodes.
"""

from typing import Dict, List, Optional, Tuple, Any
import os
import json
import joblib
import numpy as np
import pandas as pd
from datetime import datetime, timezone

from federated.models.local_model import (
    FEATURE_SCHEMA_VERSION,
    FEATURE_COUNT,
    ORDERED_FEATURE_NAMES,
)


class GlobalHotspotModel:
    """
    Consensus global hotspot prediction model.
    Maintains aggregated weights and metadata across federated rounds.
    """

    def __init__(
        self,
        version: str = "global-v1",
        weights: Optional[List[float]] = None,
        intercept: float = 0.0,
        metadata: Optional[Dict[str, Any]] = None,
    ):
        self.version = version
        self.feature_names = ORDERED_FEATURE_NAMES.copy()

        if weights is not None:
            if len(weights) != FEATURE_COUNT:
                raise ValueError(f"Global weights must have {FEATURE_COUNT} dimensions, got {len(weights)}")
            self.weights = [float(w) for w in weights]
        else:
            # Baseline initialization: zero weights
            self.weights = [0.0] * FEATURE_COUNT

        self.intercept = float(intercept)
        self.metadata = metadata or {
            "version": version,
            "roundId": "ROUND-000",
            "baseModelVersion": None,
            "participatingNodes": [],
            "totalSamples": 0,
            "aggregationStrategy": "BaselineInitialization",
            "metrics": {"mae": 0.0, "rmse": 0.0, "rocAuc": 0.5},
            "createdAt": datetime.now(timezone.utc).isoformat(),
        }

    def update_parameters(
        self,
        weights: List[float],
        intercept: float,
        version: str,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> None:
        """
        Updates global model parameters following coordinator aggregation.
        """
        if len(weights) != FEATURE_COUNT:
            raise ValueError(f"Expected {FEATURE_COUNT} weights, received {len(weights)}")

        weights_arr = np.asarray(weights, dtype=float)
        if np.isnan(weights_arr).any() or np.isinf(weights_arr).any():
            raise ValueError("Aggregated weights contain NaN or Inf values.")

        self.weights = [round(float(w), 6) for w in weights]
        self.intercept = round(float(intercept), 6)
        self.version = version

        if metadata:
            self.metadata = metadata
            self.metadata["version"] = version
            self.metadata["updatedAt"] = datetime.now(timezone.utc).isoformat()

    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """
        Performs inference using global consensus weights.
        Calculates sigmoid: P(hotspot) = 1 / (1 + exp(-(X * W + b)))
        """
        missing = [f for f in self.feature_names if f not in X.columns]
        if missing:
            raise ValueError(f"Input DataFrame is missing required features: {missing}")

        X_ordered = X[self.feature_names].fillna(0.0).astype(float).values
        W = np.asarray(self.weights, dtype=float)
        b = float(self.intercept)

        # Standard linear logits calculation
        logits = np.dot(X_ordered, W) + b
        # Numerically stable sigmoid
        logits_clamped = np.clip(logits, -20.0, 20.0)
        probs = 1.0 / (1.0 + np.exp(-logits_clamped))
        return probs

    def predict(self, X: pd.DataFrame, threshold: float = 0.5) -> np.ndarray:
        """Returns binary classification predictions."""
        probs = self.predict_proba(X)
        return (probs >= threshold).astype(int)

    def get_metadata(self) -> Dict[str, Any]:
        """Returns global model metadata and performance metrics."""
        meta = self.metadata.copy() if self.metadata else {}
        meta.update({
            "version": self.version,
            "featureSchemaVersion": FEATURE_SCHEMA_VERSION,
            "featureCount": FEATURE_COUNT,
            "intercept": self.intercept,
            "weightsSummary": {
                "mean": round(float(np.mean(self.weights)), 6),
                "std": round(float(np.std(self.weights)), 6),
                "min": round(float(np.min(self.weights)), 6),
                "max": round(float(np.max(self.weights)), 6),
            },
        })
        return meta

    def save(self, file_path: str) -> None:
        """Persists the global model artifact to disk."""
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        joblib.dump({
            "version": self.version,
            "feature_schema_version": FEATURE_SCHEMA_VERSION,
            "feature_names": self.feature_names,
            "weights": self.weights,
            "intercept": self.intercept,
            "metadata": self.metadata,
        }, file_path)

    @classmethod
    def load(cls, file_path: str) -> "GlobalHotspotModel":
        """Loads a persisted global model artifact."""
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Global model artifact not found at: {file_path}")

        data = joblib.load(file_path)
        instance = cls(
            version=data.get("version", "global-v1"),
            weights=data.get("weights"),
            intercept=data.get("intercept", 0.0),
            metadata=data.get("metadata"),
        )
        instance.feature_names = data.get("feature_names", ORDERED_FEATURE_NAMES.copy())
        return instance
