"""
AeroSentinel - Potential Hotspot Classification & Calibration
File: ai-service/ml/hotspot/model.py

Trains and calibrates interpretable classification models:
  - Baseline (Standardized Logistic Regression)
  - Random Forest Classifier (Class-weighted)
Supports Platt scaling (sigmoid) and Isotonic calibration with version-adaptive cv handling.
"""

from typing import Dict, List, Optional, Tuple, Any
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.calibration import CalibratedClassifierCV
from sklearn.metrics import precision_score, recall_score, f1_score, roc_auc_score, average_precision_score, brier_score_loss


class HotspotModelTrainer:
    def __init__(self, feature_cols: List[str], random_state: int = 42):
        self.feature_cols = feature_cols
        self.random_state = random_state
        self.models: Dict[str, Any] = {}
        self.calibrated_model: Optional[CalibratedClassifierCV] = None

    def train_models(
        self,
        X_train: pd.DataFrame,
        y_train: pd.Series,
        X_val: Optional[pd.DataFrame] = None,
        y_val: Optional[pd.Series] = None
    ) -> Dict[str, Any]:
        """Trains baseline scaled logistic regression and class-weighted Random Forest."""
        X_train_clean = X_train[self.feature_cols].fillna(0.0)

        # 1. Baseline: Standardized Logistic Regression (eliminates convergence warnings on large datasets)
        lr = make_pipeline(
            StandardScaler(),
            LogisticRegression(class_weight="balanced", max_iter=2000, random_state=self.random_state)
        )
        lr.fit(X_train_clean, y_train)
        self.models["logistic_regression"] = lr

        # 2. Main Model: Random Forest Classifier
        rf = RandomForestClassifier(
            n_estimators=100,
            max_depth=6,
            class_weight="balanced",
            random_state=self.random_state,
            n_jobs=-1
        )
        rf.fit(X_train_clean, y_train)
        self.models["random_forest"] = rf

        # 3. Probability Calibration on Validation Split
        if X_val is not None and y_val is not None and len(np.unique(y_val)) > 1:
            X_val_clean = X_val[self.feature_cols].fillna(0.0)
            
            # Scikit-learn >= 1.4 uses cv=None for prefit models, older versions use cv="prefit"
            try:
                calibrated = CalibratedClassifierCV(estimator=rf, method="sigmoid", cv=None)
                calibrated.fit(X_val_clean, y_val)
            except (ValueError, TypeError):
                try:
                    calibrated = CalibratedClassifierCV(estimator=rf, method="sigmoid", cv="prefit")
                    calibrated.fit(X_val_clean, y_val)
                except Exception:
                    # Final fallback: 3-fold CV calibration
                    calibrated = CalibratedClassifierCV(estimator=rf, method="sigmoid", cv=3)
                    calibrated.fit(X_train_clean, y_train)

            self.calibrated_model = calibrated
        else:
            self.calibrated_model = None

        return self.models

    def predict(self, X: pd.DataFrame, model_name: str = "random_forest") -> Tuple[np.ndarray, np.ndarray]:
        """
        Returns (discrete_predictions, raw_or_calibrated_probabilities).
        """
        X_in = X[self.feature_cols].fillna(0.0)
        if self.calibrated_model is not None and model_name == "random_forest":
            probs = self.calibrated_model.predict_proba(X_in)[:, 1]
            preds = (probs >= 0.50).astype(int)
            return preds, probs
        elif model_name in self.models:
            model = self.models[model_name]
            probs = model.predict_proba(X_in)[:, 1] if hasattr(model, "predict_proba") else np.zeros(len(X_in))
            preds = model.predict(X_in)
            return preds, probs
        else:
            raise ValueError(f"Model {model_name} not recognized.")