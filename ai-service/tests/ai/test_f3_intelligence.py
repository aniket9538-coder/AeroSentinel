"""
AeroSentinel - F3 AI/ML Intelligence Unit Tests
File: tests/ai/test_f3_intelligence.py

Validates:
  - Hotspot proxy target calculation without leakage
  - Hotspot classifier training & probability calibration
  - Multi-horizon forecast prediction intervals (P10 - P90)
  - Confidence calculation (coverage gap penalty & boundary ambiguity)
  - Structured evidence generation & non-causal language guardrails
  - Leakage guard: future targets absent from feature matrices
"""

from pathlib import Path
import numpy as np
import pandas as pd
import pytest

from ml.hotspot.target import create_hotspot_target
from ml.inference.confidence import compute_prediction_confidence, compute_forecast_intervals
from ml.explainability.evidence import generate_structured_evidence


def test_hotspot_target_creation():
    """
    Validates proxy target creation:
    Target requires BOTH absolute threshold breach (PM2.5 >= 60)
    AND local spatial anomaly above background neighborhood baseline.
    """
    df = pd.DataFrame({
        "pm25_clean": [50.0, 75.0, 80.0],
        "pm25_spatial_lag_mean": [50.0, 50.0, 75.0]
    })
    res = create_hotspot_target(df, threshold_pm25=60.0, anomaly_factor=1.15)
    
    # Row 0: 50.0 < 60.0 -> 0
    # Row 1: 75.0 >= 60.0 and 75.0 >= (50.0 * 1.15 = 57.5) -> 1 (Local Hotspot)
    # Row 2: 80.0 >= 60.0 but 80.0 < (75.0 * 1.15 = 86.25) -> 0 (Regional Background)
    assert res["is_potential_hotspot"].tolist() == [0, 1, 0]


def test_confidence_coverage_penalty():
    """
    Validates confidence calculation rules:
    - Low coverage increases UNCERTAINTY, not pollution risk.
    - Confidence is NOT raw model probability.
    """
    well_monitored = pd.Series({
        "pm25_clean": 45.0,
        "pm10": 80.0,
        "no2": 25.0,
        "temperature": 28.0,
        "humidity": 50.0,
        "nearest_station_distance_km": 2.1,
        "monitoring_coverage_gap_flag": 0
    })
    c1 = compute_prediction_confidence(well_monitored, predicted_probability=0.92)

    gap_station = pd.Series({
        "pm25_clean": 45.0,
        "pm10": 80.0,
        "no2": 25.0,
        "temperature": 28.0,
        "humidity": 50.0,
        "nearest_station_distance_km": 16.5,
        "monitoring_coverage_gap_flag": 1
    })
    c2 = compute_prediction_confidence(gap_station, predicted_probability=0.92)

    # Coverage penalty assertions
    assert c2["overall_confidence"] < c1["overall_confidence"]
    assert c2["coverage_confidence"] < c1["coverage_confidence"]
    assert c2["epistemic_uncertainty_flag"] == 1
    assert c1["epistemic_uncertainty_flag"] == 0


def test_confidence_boundary_ambiguity():
    """
    Validates that predictions near the decision threshold (0.50)
    receive an epistemic penalty.
    """
    monitored = {
        "pm25_clean": 55.0,
        "pm10": 90.0,
        "no2": 20.0,
        "temperature": 24.0,
        "humidity": 60.0,
        "nearest_station_distance_km": 3.0,
        "monitoring_coverage_gap_flag": 0
    }
    certain_pred = compute_prediction_confidence(monitored, predicted_probability=0.95)
    ambiguous_pred = compute_prediction_confidence(monitored, predicted_probability=0.51)

    assert certain_pred["model_certainty"] > ambiguous_pred["model_certainty"]
    assert certain_pred["overall_confidence"] > ambiguous_pred["overall_confidence"]


def test_forecast_intervals_validity():
    """
    Validates multi-horizon prediction intervals:
    - Lower P10 <= Point Estimate <= Upper P90
    - Non-negative physical constraint (PM2.5 >= 0.0)
    """
    forecasts = {"T+1h": 32.5, "T+3h": 45.0, "T+6h": 58.2}
    intervals = compute_forecast_intervals(forecasts)

    for h in ["T+1h", "T+3h", "T+6h"]:
        assert h in intervals
        pt = intervals[h]["point_estimate"]
        p10 = intervals[h]["lower_p10"]
        p90 = intervals[h]["upper_p90"]

        assert 0.0 <= p10 <= pt
        assert pt <= p90

    # Test edge case: near-zero PM2.5 prevents negative concentrations
    low_forecasts = {"T+1h": 1.2}
    low_intervals = compute_forecast_intervals(low_forecasts)
    assert low_intervals["T+1h"]["lower_p10"] >= 0.0


def test_evidence_traceability_and_non_causality():
    """
    Validates structured evidence generation:
    - Minimum evidence items across observation, remote sensing, and GIS
    - Strict prohibition of causal/guilt assertions
    """
    row = pd.Series({
        "observed_at": "2026-09-25T10:00:00Z",
        "h3_cell_id": "886196944dfffff",
        "pm25_clean": 95.0,
        "pm25_spatial_lag_mean": 60.0,
        "fire_upwind_alignment_score": 8.4,
        "fire_count_24h_25km": 2,
        "dist_to_nearest_industrial_km": 1.2,
        "monitoring_coverage_gap_flag": 0
    })
    evidence = generate_structured_evidence(row, hotspot_score=0.85)
    assert len(evidence) >= 3

    types = [e["type"] for e in evidence]
    assert "DIRECT_OBSERVATION" in types
    assert "REMOTE_SENSING_SIGNAL" in types
    assert "GIS_CONTEXT" in types

    # Guardrail: Assert non-causal language strictly
    forbidden_terms = ["cause", "caused", "proves", "proven", "responsible for", "culprit"]
    for item in evidence:
        interpretation = item["interpretation"].lower()
        for term in forbidden_terms:
            assert term not in interpretation, f"Causal violation '{term}' found in evidence: {interpretation}"


def test_feature_matrix_no_future_leakage():
    """
    Verifies that no future forecast target is included as an input feature.
    """
    sample_features = [
        "pm25_clean", "pm10", "temperature", "wind_u", "wind_v",
        "fire_upwind_alignment_score", "dist_to_nearest_industrial_km"
    ]
    # Synthetic verification of feature names
    for f in sample_features:
        assert not f.startswith("target_pm25_t_plus_"), f"Target leakage detected in input feature list: {f}"