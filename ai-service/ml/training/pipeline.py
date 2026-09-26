"""
AeroSentinel - Reusable Training Pipeline
File: ai-service/ml/training/pipeline.py
Complies with Section 39: Reusable modules for temporal splitting and model training.
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.calibration import CalibratedClassifierCV
from sklearn.linear_model import LogisticRegression
import joblib
from pathlib import Path

def chronological_split(df: pd.DataFrame, train_frac=0.70, val_frac=0.15):
    """
    Splits data strictly chronologically (Section 11/18).
    """
    df = df.sort_values("observed_at").copy()
    n = len(df)
    train_end = int(n * train_frac)
    val_end = train_end + int(n * val_frac)
    
    train_df = df.iloc[:train_end]
    val_df = df.iloc[train_end:val_end]
    test_df = df.iloc[val_end:]
    
    return train_df, val_df, test_df

def train_hotspot_classifier(X_train, y_train):
    """
    Trains the Balanced Random Forest with Platt Scaling (Section 9/25).
    """
    base_rf = RandomForestClassifier(
        n_estimators=100,
        class_weight="balanced",
        max_depth=12,
        random_state=42,
        n_jobs=-1
    )
    # Platt scaling requires cross-validation or prefit on a validation set.
    calibrated_rf = CalibratedClassifierCV(base_rf, method='sigmoid', cv=3)
    calibrated_rf.fit(X_train, y_train)
    return calibrated_rf

def train_forecast_regressors(X_train, y_train_dict):
    """
    Trains multi-horizon regressors (T+1, T+3, T+6) (Section 14/16).
    y_train_dict: dict of {horizon: y_series}
    """
    models = {}
    for horizon, y_train in y_train_dict.items():
        # Drop nulls caused by temporal shift
        valid_idx = y_train.notna()
        X_valid = X_train[valid_idx]
        y_valid = y_train[valid_idx]
        
        rf = RandomForestRegressor(
            n_estimators=100, 
            max_depth=15, 
            random_state=42, 
            n_jobs=-1
        )
        rf.fit(X_valid, y_valid)
        models[horizon] = rf
    return models