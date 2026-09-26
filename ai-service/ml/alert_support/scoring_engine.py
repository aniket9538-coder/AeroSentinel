"""
AeroSentinel - Deterministic Event Evidence Scoring Engine (F5)
File: ai-service/ml/alert_support/scoring_engine.py
Fulfills Sections 7 through 35 of the F5 Engineering Specification.
"""

import hashlib
from typing import Dict, Any, List, Optional
from datetime import datetime

from app.schemas.alert_contracts import (
    AlertSupportPayload,
    AlertSupportState,
    EvidenceConsistencyTier,
    SourceObservationState,
    MultiSourceStateMatrix,
    SourcePresenceBreakdown,
    EventEvidenceScoreBreakdown,
    AlertLeadTimeMetrics,
    EventGroupingMetadata
)
from app.utils.alert_config import alert_config, AlertScoringConfig
from ml.alert_support.recency import EvidenceRecencyHandler
from ml.alert_support.citizen_dedup import CitizenReportDeduplicator
from ml.alert_support.clustering import EventClusterer


class EventEvidenceScoringEngine:
    """
    Computes an auditable, normalized Event Evidence Score [0.0, 1.0] and classifies
    the event into INSUFFICIENT_EVIDENCE, MONITOR, or ALERT_CANDIDATE.
    """

    def __init__(self, config: Optional[AlertScoringConfig] = None):
        self.config = config or alert_config

    def generate_alert_support_payload(
        self,
        f3_payload: Dict[str, Any],
        f4_explanation: Optional[Dict[str, Any]] = None,
        neighbor_payloads: Optional[List[Dict[str, Any]]] = None,
        raw_citizen_reports: Optional[List[Dict[str, Any]]] = None
    ) -> AlertSupportPayload:
        h3_cell = f3_payload.get("h3_cell_id", "unknown")
        timestamp = f3_payload.get("timestamp", datetime.utcnow().isoformat())
        hotspot = f3_payload.get("hotspot", {})
        forecast = f3_payload.get("forecast", {})
        confidence = f3_payload.get("confidence", {})
        evidence_list = f3_payload.get("evidence", [])

        # 1. Citizen Deduplication (Section 31 & 32)
        valid_citizen_reports = CitizenReportDeduplicator.deduplicate_reports(raw_citizen_reports or [])
        citizen_count = len(valid_citizen_reports)

        # 2. 4-Valued Multi-Source Representation (Section 19 & 20)
        source_matrix = MultiSourceStateMatrix()
        sources_present = SourcePresenceBreakdown()

        # Ground Observations
        ground_items = [
            e for e in evidence_list
            if "GROUND" in (e.get("category") or e.get("type", "")).upper()
            or "CAAQMS" in str(e.get("source", "")).upper()
        ]
        if ground_items:
            sources_present.ground_sensor_present = True
            source_matrix.ground_sensor = (
                SourceObservationState.SUPPORTED
                if any(float(e.get("value", 0)) > 60.0 for e in ground_items)
                else SourceObservationState.NOT_DETECTED
            )
        else:
            source_matrix.ground_sensor = SourceObservationState.UNAVAILABLE

        # Active Fire Telemetry
        fire_items = [
            e for e in evidence_list
            if "FIRE" in str(e.get("metric", "")).lower()
            or "FIRMS" in str(e.get("source", "")).upper()
        ]
        if fire_items:
            sources_present.fire_present = True
            source_matrix.fire = (
                SourceObservationState.SUPPORTED
                if any(float(e.get("value", 0)) > 0 for e in fire_items)
                else SourceObservationState.NOT_DETECTED
            )
        else:
            source_matrix.fire = SourceObservationState.UNAVAILABLE

        # Satellite Atmospheric Columns
        sat_items = [
            e for e in evidence_list
            if "SATELLITE" in (e.get("category") or "").upper()
            or "SENTINEL" in str(e.get("source", "")).upper()
        ]
        if sat_items:
            sources_present.satellite_present = True
            if any(
                "nominal" in str(e.get("attribution_note", "")).lower()
                or "no detectable" in str(e.get("attribution_note", "")).lower()
                for e in sat_items
            ):
                source_matrix.satellite = SourceObservationState.NOT_DETECTED
            else:
                source_matrix.satellite = SourceObservationState.SUPPORTED
        else:
            source_matrix.satellite = SourceObservationState.UNAVAILABLE

        # Meteorology
        meteo_items = [
            e for e in evidence_list
            if "METEO" in (e.get("category") or "").upper()
            or "ERA5" in str(e.get("source", "")).upper()
        ]
        if meteo_items:
            sources_present.meteorology_present = True
            source_matrix.meteorology = SourceObservationState.SUPPORTED
        else:
            source_matrix.meteorology = SourceObservationState.UNAVAILABLE

        # GIS Context
        gis_items = [
            e for e in evidence_list
            if "GIS" in (e.get("category") or "").upper()
            or "MIDC" in str(e.get("source", "")).upper()
        ]
        if gis_items:
            sources_present.gis_present = True
            source_matrix.gis = SourceObservationState.SUPPORTED
        else:
            source_matrix.gis = SourceObservationState.UNAVAILABLE

        # Citizen Observations
        has_citizen_visual = bool(f4_explanation and f4_explanation.get("visual_evidence"))
        if citizen_count > 0 or has_citizen_visual:
            sources_present.citizen_present = True
            source_matrix.citizen = SourceObservationState.SUPPORTED
        else:
            source_matrix.citizen = SourceObservationState.UNAVAILABLE

        # Machine Learning Inference
        if hotspot.get("calibrated_hotspot_probability") is not None:
            sources_present.ml_present = True
            source_matrix.ml_model = SourceObservationState.SUPPORTED
        else:
            source_matrix.ml_model = SourceObservationState.UNAVAILABLE

        # 3. Evidence Completeness (Section 22)
        tracked_physical_tiers = [
            source_matrix.ground_sensor,
            source_matrix.satellite,
            source_matrix.fire,
            source_matrix.meteorology,
            source_matrix.gis,
            source_matrix.citizen
        ]
        available_tier_count = sum(1 for s in tracked_physical_tiers if s != SourceObservationState.UNAVAILABLE)
        evidence_completeness = round(available_tier_count / len(tracked_physical_tiers), 3)

        # 4. Evidence Recency Factor [0.10, 1.0] (Section 14 & 15)
        recency_factor = EvidenceRecencyHandler.evaluate_payload_recency(evidence_list, timestamp)

        # 5. Observation Strength Component [0.0, 1.0] (Section 11)
        ground_obs_values = [
            float(item.get("value", 0.0))
            for item in ground_items
            if "pm25" in str(item.get("metric", "")).lower()
        ]
        if ground_obs_values:
            max_pm25 = max(ground_obs_values)
            observation_strength = min(1.0, max(0.0, (max_pm25 - 30.0) / 270.0))
        else:
            observation_strength = 0.20 if hotspot.get("is_hotspot") else 0.0

        # 6. ML & Forecast Support Component [0.0, 1.0] (Section 11 & 35)
        prob = float(hotspot.get("calibrated_hotspot_probability", 0.0))
        t1 = float(forecast.get("pm25_t_plus_1h", 0.0))
        t3 = float(forecast.get("pm25_t_plus_3h", 0.0))
        t6 = float(forecast.get("pm25_t_plus_6h", 0.0))
        fc_peak = max(t1, t3, t6)
        fc_normalized = min(1.0, max(0.0, (fc_peak - 30.0) / 270.0))
        overall_conf = float(confidence.get("overall_confidence", 0.50))
        ml_forecast_support = round(0.40 * prob + 0.40 * fc_normalized + 0.20 * overall_conf, 3)

        # 7. Multi-Source Agreement Component [0.0, 1.0] (Section 19)
        supported_physical_count = sum(
            1 for s in [
                source_matrix.ground_sensor,
                source_matrix.satellite,
                source_matrix.fire,
                source_matrix.gis,
                source_matrix.citizen
            ]
            if s == SourceObservationState.SUPPORTED
        )
        multi_source_agreement = round(min(1.0, supported_physical_count / 3.0), 3)

        # 8. Spatial Consistency Component [0.0, 1.0] & Clustering (Section 18 & 33)
        cluster_cells = EventClusterer.group_cells(h3_cell, neighbor_payloads or [])
        if neighbor_payloads:
            elevated_neighbors = sum(
                1 for n in neighbor_payloads
                if n.get("hotspot", {}).get("is_hotspot", False)
            )
            spatial_consistency = round(min(1.0, elevated_neighbors / len(neighbor_payloads)), 3)
        else:
            coverage_conf = float(confidence.get("spatial_coverage_confidence", 0.50))
            spatial_consistency = round(coverage_conf, 3)

        # 9. Temporal Persistence Component [0.0, 1.0] (Section 17 & 35)
        elevated_horizons = sum(1 for val in [t1, t3, t6] if val >= 60.0)
        temporal_persistence = round(elevated_horizons / 3.0, 3)

        # 10. Conflict Detection & Consistency Classification (Section 18 & 21)
        has_monitoring_gap = any(
            item.get("category") == "MONITORING_COVERAGE"
            or item.get("metric") == "monitoring_coverage_gap_flag"
            for item in evidence_list
        )
        conflicting_satellite = (
            source_matrix.ground_sensor == SourceObservationState.SUPPORTED
            and source_matrix.satellite == SourceObservationState.NOT_DETECTED
        )

        conflicting_notes = []
        unavailable_sources = []
        if source_matrix.satellite == SourceObservationState.UNAVAILABLE:
            unavailable_sources.append("Sentinel-5P tropospheric column unavailable (cloud filter or night window)")
        if source_matrix.citizen == SourceObservationState.UNAVAILABLE:
            unavailable_sources.append("No citizen ground reports registered for active cell")

        if has_monitoring_gap or overall_conf < 0.40:
            evidence_consistency = EvidenceConsistencyTier.INSUFFICIENT_EVIDENCE
            conflict_penalty = 0.35
            conflicting_notes.append("Severe spatial distance from reference sensors (>10km); elevated interpolation variance")
        elif conflicting_satellite:
            evidence_consistency = EvidenceConsistencyTier.CONFLICTING
            conflict_penalty = self.config.conflict_penalty_multiplier
            conflicting_notes.append("Ground sensor indicates elevated PM2.5 while satellite column remains nominal")
        elif multi_source_agreement >= 0.60:
            evidence_consistency = EvidenceConsistencyTier.CONSISTENT
            conflict_penalty = 0.0
        else:
            evidence_consistency = EvidenceConsistencyTier.PARTIALLY_CONSISTENT
            conflict_penalty = 0.10

        # 11. Final Evidence Score [0.0, 1.0] with Recency Scaling
        raw_weighted_score = (
            self.config.weight_observation * observation_strength +
            self.config.weight_ml_forecast * ml_forecast_support +
            self.config.weight_multi_source * multi_source_agreement +
            self.config.weight_spatial * spatial_consistency +
            self.config.weight_temporal * temporal_persistence
        )
        penalized_score = max(0.0, (raw_weighted_score * recency_factor) - conflict_penalty)
        final_evidence_score = round(min(1.0, penalized_score), 3)

        # 12. Alert Support State Classification (Section 23)
        if (
            evidence_consistency == EvidenceConsistencyTier.INSUFFICIENT_EVIDENCE
            or evidence_completeness < self.config.min_completeness_for_alert
            or final_evidence_score < self.config.monitor_threshold
        ):
            alert_support_state = AlertSupportState.INSUFFICIENT_EVIDENCE
        elif final_evidence_score >= self.config.alert_candidate_threshold:
            alert_support_state = AlertSupportState.ALERT_CANDIDATE
        else:
            alert_support_state = AlertSupportState.MONITOR

        # 13. Deduplication Hash & Event ID (Section 34)
        time_clean = timestamp.replace("-", "").replace(":", "").replace("T", "")[:10]
        dedup_raw = f"{h3_cell}:{time_clean}:{self.config.scoring_version}"
        dedup_hash = hashlib.sha256(dedup_raw.encode()).hexdigest()[:8]
        event_id = f"EVT-{h3_cell[:8]}-{time_clean}-{dedup_hash}"

        score_breakdown = EventEvidenceScoreBreakdown(
            observation_strength=round(observation_strength, 3),
            ml_forecast_support=round(ml_forecast_support, 3),
            multi_source_agreement=round(multi_source_agreement, 3),
            spatial_consistency=round(spatial_consistency, 3),
            temporal_persistence=round(temporal_persistence, 3),
            recency_factor=recency_factor,
            conflict_penalty=round(conflict_penalty, 3),
            evidence_completeness=evidence_completeness,
            final_evidence_score=final_evidence_score
        )

        p10 = float(forecast.get("uncertainty_lower_bound_p10", 0.0))
        p90 = float(forecast.get("uncertainty_upper_bound_p90", 0.0))
        lead_time_metrics = AlertLeadTimeMetrics(
            forecast_horizon_hours=6,
            predicted_persistence_hours=float(elevated_horizons * 2.0),
            lead_time_to_peak_hours=1 if t1 >= max(t3, t6) else (3 if t3 >= t6 else 6),
            peak_predicted_pm25=round(fc_peak, 1),
            uncertainty_range_width=round(abs(p90 - p10), 1)
        )

        grouping = EventGroupingMetadata(
            canonical_event_id=event_id,
            primary_h3_cell=h3_cell,
            cluster_h3_cells=cluster_cells,
            time_window_start=timestamp,
            time_window_end=timestamp,
            is_primary_cell=True,
            deduplication_hash=dedup_hash,
            cluster_size=len(cluster_cells)
        )

        supporting_signals = [
            item.get("attribution_note", "")
            for item in evidence_list
            if item.get("attribution_note")
        ]

        public_summary = f4_explanation.get("event_summary_public") if f4_explanation else None
        analyst_summary = f4_explanation.get("event_summary_analyst") if f4_explanation else None

        return AlertSupportPayload(
            event_id=event_id,
            timestamp=timestamp,
            h3_cell_id=h3_cell,
            alert_support_state=alert_support_state,
            evidence_score=final_evidence_score,
            evidence_completeness=evidence_completeness,
            evidence_consistency=evidence_consistency,
            score_breakdown=score_breakdown,
            source_matrix=source_matrix,
            sources_present=sources_present,
            lead_time_metrics=lead_time_metrics,
            grouping=grouping,
            supporting_signals=supporting_signals,
            unavailable_sources=unavailable_sources,
            conflicting_notes=conflicting_notes,
            gemini_summary_public=public_summary,
            gemini_summary_analyst=analyst_summary,
            scoring_version=self.config.scoring_version,
            model_version=f3_payload.get("model_version", "v1.0.0"),
            feature_version="v1.0.0"
        )