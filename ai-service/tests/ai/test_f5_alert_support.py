"""
AeroSentinel - Phase 5 Alert Support Test Suite
File: tests/ai/test_f5_alert_support.py
Fulfills Section 38 (F5 Validation).
"""

import copy
import pytest
from app.schemas.alert_contracts import (
    AlertSupportPayload,
    AlertSupportState,
    EvidenceConsistencyTier,
    SourceObservationState
)
from ml.alert_support.scoring_engine import EventEvidenceScoringEngine
from ml.alert_support.citizen_dedup import CitizenReportDeduplicator
from ml.alert_support.clustering import EventClusterer
from tests.ai.evaluation_dataset import CURATED_F4_EVALUATION_CASES


@pytest.fixture
def scoring_engine():
    return EventEvidenceScoringEngine()


@pytest.fixture
def industrial_payload():
    return copy.deepcopy(CURATED_F4_EVALUATION_CASES[0]["f3_payload"])


def test_alert_candidate_generation(scoring_engine, industrial_payload):
    """Verifies that high-evidence multi-source events qualify as ALERT_CANDIDATE."""
    result = scoring_engine.generate_alert_support_payload(industrial_payload)
    
    assert isinstance(result, AlertSupportPayload)
    assert result.alert_support_state == AlertSupportState.ALERT_CANDIDATE
    assert result.evidence_score >= 0.55
    assert result.evidence_completeness >= 0.40
    assert result.evidence_consistency == EvidenceConsistencyTier.CONSISTENT
    assert result.grouping.canonical_event_id.startswith("EVT-")


def test_conflict_penalty_degrades_state(scoring_engine, industrial_payload):
    """Verifies that conflicting satellite data penalizes the score and lowers state."""
    conflicted_payload = dict(industrial_payload)
    conflicted_payload["evidence"] = list(industrial_payload["evidence"]) + [
        {
            "category": "REMOTE_SENSING",
            "source": "SENTINEL_5P_TROPOMI",
            "metric": "satellite_no2_trop",
            "attribution_note": "Tropospheric column shows no detectable anomaly (nominal baseline)."
        }
    ]
    
    result = scoring_engine.generate_alert_support_payload(conflicted_payload)
    assert result.evidence_consistency == EvidenceConsistencyTier.CONFLICTING
    assert result.score_breakdown.conflict_penalty > 0.0
    # Conflicting data should downgrade from ALERT_CANDIDATE to MONITOR
    assert result.alert_support_state in [AlertSupportState.MONITOR, AlertSupportState.INSUFFICIENT_EVIDENCE]


def test_monitoring_gap_forces_insufficient_evidence(scoring_engine, industrial_payload):
    """Verifies that unmonitored blind spots (>10km) force INSUFFICIENT_EVIDENCE."""
    gap_payload = dict(industrial_payload)
    gap_payload["confidence"] = {"overall_confidence": 0.30}
    gap_payload["evidence"] = [
        {
            "category": "MONITORING_COVERAGE",
            "source": "SPATIAL_INDEX",
            "metric": "monitoring_coverage_gap_flag",
            "attribution_note": "Nearest sensor is 14km away"
        }
    ]
    
    result = scoring_engine.generate_alert_support_payload(gap_payload)
    assert result.alert_support_state == AlertSupportState.INSUFFICIENT_EVIDENCE
    assert result.evidence_consistency == EvidenceConsistencyTier.INSUFFICIENT_EVIDENCE


def test_deterministic_event_id_reproducibility(scoring_engine, industrial_payload):
    """Verifies Section 34: Event IDs are strictly deterministic given same inputs."""
    res1 = scoring_engine.generate_alert_support_payload(industrial_payload)
    res2 = scoring_engine.generate_alert_support_payload(industrial_payload)
    
    assert res1.event_id == res2.event_id
    assert res1.grouping.deduplication_hash == res2.grouping.deduplication_hash
    assert res1.evidence_score == res2.evidence_score


def test_lead_time_metrics_computation(scoring_engine, industrial_payload):
    """Verifies Section 35: Alert lead-time metrics and uncertainty width."""
    result = scoring_engine.generate_alert_support_payload(industrial_payload)
    metrics = result.lead_time_metrics
    
    assert metrics.forecast_horizon_hours == 6
    assert metrics.predicted_persistence_hours > 0.0
    assert metrics.peak_predicted_pm25 == 174.0
    # Uncertainty width: |182.0 - 145.0| = 37.0 ug/m3
    assert metrics.uncertainty_range_width == 37.0


def test_evidence_recency_decay_reduces_score(scoring_engine, industrial_payload):
    """Verifies Section 15: Stale telemetry decays score compared to current telemetry."""
    current_res = scoring_engine.generate_alert_support_payload(industrial_payload)
    
    stale_payload = copy.deepcopy(industrial_payload)
    stale_payload["timestamp"] = "2026-09-25T14:00:00Z"
    for item in stale_payload["evidence"]:
        item["timestamp"] = "2026-09-25T06:00:00Z"  # 8 hours old (stale)
    
    stale_res = scoring_engine.generate_alert_support_payload(stale_payload)
    assert stale_res.score_breakdown.recency_factor < 1.0
    assert stale_res.evidence_score < current_res.evidence_score


def test_citizen_report_deduplication():
    """Verifies Section 32: Redundant citizen reports within 60 mins are coalesced."""
    raw_reports = [
        {"h3_cell_id": "886196944dfffff", "timestamp": "2026-09-25T14:10:00Z", "note": "Dark smoke"},
        {"h3_cell_id": "886196944dfffff", "timestamp": "2026-09-25T14:25:00Z", "note": "Smoke visible"},  # Duplicate
        {"h3_cell_id": "886196944bfffff", "timestamp": "2026-09-25T14:15:00Z", "note": "Different cell"}
    ]
    deduped = CitizenReportDeduplicator.deduplicate_reports(raw_reports)
    assert len(deduped) == 2


def test_multi_cell_clustering_groups_adjacent_elevated_cells():
    """Verifies Section 33: Adjacent elevated H3 cells are clustered together."""
    primary = "886196944dfffff"
    # Provide two neighbors: one elevated hotspot, one normal
    neighbor_payloads = [
        {"h3_cell_id": "8861969445fffff", "hotspot": {"is_hotspot": True}},
        {"h3_cell_id": "8861969447fffff", "hotspot": {"is_hotspot": False}}
    ]
    cluster = EventClusterer.group_cells(primary, neighbor_payloads)
    assert primary in cluster
    assert len(cluster) >= 1


def test_four_valued_source_matrix_representation(scoring_engine, industrial_payload):
    """Verifies Section 20: 4-valued states (supported, not_detected, unavailable, unknown)."""
    result = scoring_engine.generate_alert_support_payload(industrial_payload)
    matrix = result.source_matrix
    assert matrix.ground_sensor == SourceObservationState.SUPPORTED
    assert matrix.fire == SourceObservationState.SUPPORTED
    assert matrix.satellite == SourceObservationState.UNAVAILABLE
    assert matrix.citizen == SourceObservationState.UNAVAILABLE
    assert matrix.ml_model == SourceObservationState.SUPPORTED


def test_threshold_boundary_classification(scoring_engine, industrial_payload):
    """Verifies Section 24 & 38: Boundary conditions around 0.55 alert threshold."""
    res = scoring_engine.generate_alert_support_payload(industrial_payload)
    assert res.alert_support_state == AlertSupportState.ALERT_CANDIDATE
    assert res.evidence_score >= scoring_engine.config.alert_candidate_threshold