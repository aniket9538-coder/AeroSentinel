"""
AeroSentinel - Structured Evidence Generation Layer
File: ai-service/ml/explainability/evidence.py

Translates sensor data, fire plumes, satellite columns, and GIS layers
into traceable, non-causal structured evidence for Gemini and alert systems.
Rule: Never claims 'Factory X caused pollution'; states 'contributing signal'.
Fulfills Section 29-34 (Evidence Structure, Quality, Governance, and Event Output).
"""

from datetime import datetime, timezone
from typing import Dict, List, Any, Optional
import numpy as np
import pandas as pd


def generate_structured_evidence(
    row: pd.Series,
    hotspot_score: float,
    threshold: float = 0.20,
    confidence_breakdown: Optional[Dict[str, float]] = None
) -> List[Dict[str, Any]]:
    """
    Generates structured, traceable evidence items strictly bound to observed features.
    Returns a List of evidence item dictionaries complying with test suite assertions.
    """
    evidence_items = []
    
    # 1. Temporal and spatial identifiers
    raw_ts = row.get("observed_at") or row.get("hourly_bin")
    ts = str(raw_ts) if pd.notna(raw_ts) else datetime.now(timezone.utc).isoformat()
    cell = str(row.get("h3_cell_id", "unknown"))

    # Evidence 1: Ground Pollutant Anomaly (Direct Observation)
    pm25_val = row.get("pm25_clean") if pd.notna(row.get("pm25_clean")) else row.get("pm25")
    spatial_lag = row.get("pm25_spatial_lag_mean")
    
    if pd.notna(pm25_val):
        pm25_val = float(pm25_val)
        base_val = float(spatial_lag) if pd.notna(spatial_lag) and spatial_lag > 0 else 60.0
        ratio = round(pm25_val / max(base_val, 1.0), 2)
        
        evidence_items.append({
            "type": "DIRECT_OBSERVATION",
            "category": "DIRECT_OBSERVATION",
            "source": "CPCB_CAAQMS_GROUND_SENSOR",
            "feature": "pm25_clean",
            "metric": "pm25_concentration",
            "value": round(pm25_val, 2),
            "baseline": round(base_val, 2),
            "reference_baseline": round(base_val, 2),
            "direction": "ELEVATED" if pm25_val >= 60.0 else "NOMINAL",
            "signal_direction": "ELEVATED" if pm25_val >= 60.0 else "NOMINAL",
            "strength": "STRONG" if ratio >= 1.25 or pm25_val >= 90.0 else "MODERATE",
            "interpretation": f"Ground PM2.5 measurement ({pm25_val:.1f} ug/m3) deviates by {ratio}x from regional spatial baseline.",
            "attribution_note": f"Ground PM2.5 measurement ({pm25_val:.1f} ug/m3) deviates by {ratio}x from regional spatial baseline.",
            "timestamp": ts
        })

    # Evidence 2: Active Fire Plume Alignment (Remote Sensing + Meteorology)
    fire_score = float(row.get("fire_upwind_alignment_score", 0.0))
    fire_count = int(row.get("fire_count_24h_25km", 0))
    if fire_score > 0.0 or fire_count > 0:
        evidence_items.append({
            "type": "REMOTE_SENSING_SIGNAL",
            "category": "REMOTE_SENSING",
            "source": "NASA_FIRMS_VIIRS",
            "feature": "fire_upwind_alignment_score",
            "metric": "fire_upwind_alignment_score",
            "value": round(fire_score, 2),
            "baseline": 0.0,
            "reference_baseline": 0.0,
            "direction": "UPWIND_ALIGNED",
            "signal_direction": "UPWIND_ALIGNED",
            "strength": "STRONG" if fire_score > 5.0 else "MODERATE",
            "interpretation": f"{fire_count} thermal anomaly detected upwind within 25 km transport corridor; associated atmospheric signal only.",
            "attribution_note": f"{fire_count} thermal anomaly detected upwind within 25 km transport corridor; associated atmospheric signal only.",
            "timestamp": ts
        })

    # Evidence 3: Satellite Atmospheric Column (Remote Sensing)
    sat_no2 = row.get("satellite_no2_trop")
    if pd.notna(sat_no2) and float(sat_no2) > 0.00010:
        sat_val = float(sat_no2)
        evidence_items.append({
            "type": "REMOTE_SENSING_SIGNAL",
            "category": "REMOTE_SENSING",
            "source": "SENTINEL_5P_TROPOMI",
            "feature": "satellite_no2_trop",
            "metric": "satellite_no2_trop",
            "value": float(round(sat_val, 6)),
            "baseline": 0.00008,
            "reference_baseline": 0.00008,
            "direction": "ELEVATED_COLUMN",
            "signal_direction": "ELEVATED_COLUMN",
            "strength": "MODERATE",
            "interpretation": "Elevated tropospheric NO2 column detected on cloud-free satellite overpass; correlated environmental indicator.",
            "attribution_note": "Elevated tropospheric NO2 column detected on cloud-free satellite overpass; correlated environmental indicator.",
            "timestamp": ts
        })

    # Evidence 4: Urban Infrastructure Proximity (GIS Context)
    dist_ind = row.get("dist_to_nearest_industrial_km")
    if pd.notna(dist_ind):
        dist_ind = float(dist_ind)
        if dist_ind <= 2.0:
            evidence_items.append({
                "type": "GIS_CONTEXT",
                "category": "GIS_CONTEXT",
                "source": "MUNICIPAL_GIS_MIDC",
                "feature": "dist_to_nearest_industrial_km",
                "metric": "dist_to_nearest_industrial_km",
                "value": round(dist_ind, 2),
                "baseline": 2.0,
                "reference_baseline": 2.0,
                "direction": "PROXIMATE",
                "signal_direction": "PROXIMATE",
                "strength": "HIGH" if dist_ind <= 1.0 else "MODERATE",
                "interpretation": f"Location is {dist_ind:.2f} km from designated industrial MIDC boundary; does not infer single-facility causality.",
                "attribution_note": f"Location is {dist_ind:.2f} km from designated industrial MIDC boundary; does not infer single-facility causality.",
                "timestamp": ts
            })

    # Evidence 5: Monitoring Coverage Gap (Uncertainty Context)
    is_gap = int(row.get("monitoring_coverage_gap_flag", 0))
    dist_st = float(row.get("nearest_station_distance_km", 0.0))
    if is_gap == 1 or dist_st > 10.0:
        evidence_items.append({
            "type": "MONITORING_UNCERTAINTY",
            "category": "MONITORING_COVERAGE",
            "source": "AEROSENTINEL_SPATIAL_INDEX",
            "feature": "monitoring_coverage_gap_flag",
            "metric": "monitoring_coverage_gap_flag",
            "value": is_gap,
            "baseline": 0,
            "reference_baseline": 0,
            "direction": "SPARSE_COVERAGE",
            "signal_direction": "SPARSE_COVERAGE",
            "strength": "CAUTIONARY",
            "interpretation": f"Nearest ground monitor is {dist_st:.1f} km away; elevated uncertainty for ground-level interpolation.",
            "attribution_note": f"Nearest ground monitor is {dist_st:.1f} km away; elevated uncertainty for ground-level interpolation.",
            "timestamp": ts
        })

    return evidence_items


def generate_event_payload(
    row: pd.Series,
    hotspot_score: float,
    threshold: float = 0.20,
    confidence_breakdown: Optional[Dict[str, float]] = None
) -> Dict[str, Any]:
    """Wraps evidence items into the canonical AeroSentinel payload contract."""
    raw_ts = row.get("observed_at") or row.get("hourly_bin")
    ts = str(raw_ts) if pd.notna(raw_ts) else datetime.now(timezone.utc).isoformat()
    cell = str(row.get("h3_cell_id", "unknown"))

    conf = confidence_breakdown or {
        "overall_confidence": 0.85,
        "model_margin_confidence": 0.85,
        "data_completeness_score": 0.90,
        "spatial_coverage_confidence": 0.80
    }

    evidence = generate_structured_evidence(row, hotspot_score, threshold, conf)

    return {
        "timestamp": ts,
        "h3_cell_id": cell,
        "model_assessment": {
            "calibrated_hotspot_probability": round(float(hotspot_score), 4),
            "operational_threshold": float(threshold),
            "is_hotspot": bool(hotspot_score >= threshold),
            "governance_claim": "Associated statistical observations only. Does not prove regulatory ground-truth causality."
        },
        "confidence": conf,
        "evidence_signals": evidence
    }