"""
AeroSentinel - F3 AI/ML Intelligence Unit Tests
File: tests/ai/test_f3_ml.py
Validates:
  - Hotspot classification calibration and threshold adherence
  - Multi-horizon forecasting point estimates and uncertainty bounds
  - Composite confidence scoring (penalizing monitoring gaps and ambiguity)
  - Non-causal evidence contract adherence (Section 29-34)
"""

from pathlib import Path
import joblib
import numpy as np
import pandas as pd
import pytest

from ml.evaluation.evaluator import (
    evaluate_hotspot_classifier,
    evaluate_forecaster_horizon,
    compute_hyperlocal_confidence,
)
from ml.explainability.evidence import generate_structured_evidence, generate_event_payload
from app.schemas.contracts import AeroSentinelInferencePayload


@pytest.fixture(scope="module")
def artifacts():
    repo_root = Path(__file__).resolve().parent.parent.parent
    artifacts_dir = repo_root / "models" / "artifacts"
    if not (artifacts_dir / "hotspot_classifier_v1.joblib").exists():
        pytest.skip("Model artifacts not built yet.")
    hotspot_art = joblib.load(artifacts_dir / "hotspot_classifier_v1.joblib")
    forecast_art = joblib.load(artifacts_dir / "forecast_regressors_v1.joblib")
    return hotspot_art, forecast_art


def test_hotspot_classifier_inference(artifacts):
    hotspot_art, _ = artifacts
    model = hotspot_art["model"]
    features = hotspot_art["feature_cols"]

    # Synthetic sample row
    sample = pd.DataFrame([{f: 1.0 for f in features}])
    probs = model.predict_proba(sample)[:, 1]

    assert len(probs) == 1
    assert 0.0 <= probs[0] <= 1.0
    assert hotspot_art["operational_threshold"] == 0.20


def test_forecaster_inference_and_intervals(artifacts):
    _, forecast_art = artifacts
    models = forecast_art["models"]
    features = forecast_art["feature_cols"]
    residuals = forecast_art["residuals"]

    sample = pd.DataFrame([{f: 1.0 for f in features}])

    for h in [1, 3, 6]:
        point = float(models[h].predict(sample)[0])
        p10 = residuals[h]["p10"]
        p90 = residuals[h]["p90"]

        lower = max(0.0, point + p10)
        upper = point + p90

        assert lower <= upper
        assert p10 <= p90


def test_confidence_computation_logic():
    # Situation A: Clear margin, high completeness, close to sensor
    conf_high = compute_hyperlocal_confidence(
        calibrated_prob=0.90,
        operational_threshold=0.20,
        feature_completeness=1.0,
        nearest_station_distance_km=1.5,
        monitoring_coverage_gap_flag=0,
    )

    # Situation B: Ambiguous probability, low completeness, station far away (gap)
    conf_low = compute_hyperlocal_confidence(
        calibrated_prob=0.22,
        operational_threshold=0.20,
        feature_completeness=0.5,
        nearest_station_distance_km=18.0,
        monitoring_coverage_gap_flag=1,
    )

    assert conf_high["overall_confidence"] > conf_low["overall_confidence"]
    assert conf_low["spatial_coverage_confidence"] < conf_high["spatial_coverage_confidence"]
    assert conf_low["model_margin_confidence"] < conf_high["model_margin_confidence"]


def test_evidence_generation_contract():
    sample_row = pd.Series({
        "observed_at": "2026-03-01 10:00:00+00:00",
        "h3_cell_id": "886196944dfffff",
        "pm25_clean": 85.0,
        "pm25_spatial_lag_mean": 45.0,
        "fire_upwind_alignment_score": 8.5,
        "fire_count_24h_25km": 2,
        "dist_to_nearest_industrial_km": 1.2,
        "nearest_station_distance_km": 12.0,
        "monitoring_coverage_gap_flag": 1,
    })

    conf = {
        "overall_confidence": 0.72,
        "model_margin_confidence": 0.80,
        "data_completeness_score": 0.90,
        "spatial_coverage_confidence": 0.50,
    }

    # 1. Test raw list returned by generate_structured_evidence
    evidence_list = generate_structured_evidence(
        row=sample_row,
        hotspot_score=0.45,
        threshold=0.20,
        confidence_breakdown=conf,
    )

    assert isinstance(evidence_list, list)
    assert len(evidence_list) >= 3

    categories = [item["category"] for item in evidence_list]
    assert "DIRECT_OBSERVATION" in categories
    assert "REMOTE_SENSING" in categories
    assert "GIS_CONTEXT" in categories
    assert "MONITORING_COVERAGE" in categories

    for item in evidence_list:
        note = item["attribution_note"].lower()
        assert "caused" not in note
        assert "proves" not in note

    # 2. Test full event payload contract assembly
    payload = generate_event_payload(
        row=sample_row,
        hotspot_score=0.45,
        threshold=0.20,
        confidence_breakdown=conf,
    )

    assert payload["h3_cell_id"] == "886196944dfffff"
    assert payload["model_assessment"]["is_hotspot"] is True
    assert "associated" in payload["model_assessment"]["governance_claim"].lower()
    assert len(payload["evidence_signals"]) == len(evidence_list)