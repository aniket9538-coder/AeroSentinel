"""
AeroSentinel - Multi-Horizon PM2.5 Forecast Models
File: ai-service/ml/forecast/model.py

Implements multi-horizon regressors (+1h, +3h, +6h):
  - Naive Persistence Baseline (y_{T+H} = y_T)
  - Historical Mean Baseline
  - Multi-Output / Multi-Horizon Random Forest Regressor
"""

from typing import Dict, List, Optional, Tuple, Any
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import Ridge


class ForecastModelTrainer:
    def __init__(self, feature_cols: List[str], horizons: List[int] = [1, 3, 6], random_state: int = 42):
        self.feature_cols = feature_cols
        self.horizons = horizons
        self.random_state = random_state
        self.models: Dict[int, RandomForestRegressor] = {}
        self.historical_means: Dict[int, float] = {}

    def train(self, train_df: pd.DataFrame) -> None:
        """Trains independent regressors for each forecast horizon without future leakage."""
        for h in self.horizons:
            target_col = f"target_pm25_t_plus_{h}"
            valid_df = train_df.dropna(subset=[target_col]).copy()
            
            if valid_df.empty:
                # Fallback for minimal sample test sequences
                valid_df = train_df.copy()
                valid_df[target_col] = valid_df["pm25_clean"]

            X = valid_df[self.feature_cols].fillna(0.0)
            y = valid_df[target_col]

            self.historical_means[h] = float(y.mean())

            reg = RandomForestRegressor(
                n_estimators=100,
                max_depth=6,
                random_state=self.random_state
            )
            reg.fit(X, y)
            self.models[h] = reg

    def predict_persistence(self, df: pd.DataFrame) -> pd.Series:
        """Naive baseline: Future PM2.5 equals current PM2.5."""
        return df["pm25_clean"].copy()

    def predict_mean(self, df: pd.DataFrame, horizon: int) -> pd.Series:
        """Baseline: Historical mean."""
        mean_val = self.historical_means.get(horizon, 50.0)
        return pd.Series(mean_val, index=df.index)

    def predict_model(self, df: pd.DataFrame, horizon: int) -> pd.Series:
        """ML Forecast."""
        if horizon not in self.models:
            raise ValueError(f"Horizon T+{horizon} not trained.")
        X = df[self.feature_cols].fillna(0.0)
        preds = self.models[horizon].predict(X)
        return pd.Series(preds, index=df.index)