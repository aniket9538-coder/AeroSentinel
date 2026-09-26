"""
AeroSentinel - Curated F4 Evaluation Cases
File: tests/ai/evaluation_dataset.py
Fulfills Section 34 (Curated Vision & Multimodal Test Set) and Section 35 (Gemini Evaluation).
"""

CURATED_F4_EVALUATION_CASES = [
    {
        "id": "CASE_01_INDUSTRIAL_HOTSPOT",
        "description": "Severe ground PM2.5 elevation near MIDC industrial cluster with upwind active fire.",
        "f3_payload": {
            "timestamp": "2026-09-25T14:00:00Z",
            "h3_cell_id": "886196944dfffff",
            "hotspot": {
                "calibrated_hotspot_probability": 0.88,
                "operational_threshold": 0.20,
                "is_hotspot": True,
                "governance_claim": "Associated statistical observations only."
            },
            "forecast": {
                "pm25_t_plus_1h": 152.0,
                "pm25_t_plus_3h": 168.5,
                "pm25_t_plus_6h": 174.0,
                "uncertainty_lower_bound_p10": 145.0,
                "uncertainty_upper_bound_p90": 182.0
            },
            "confidence": {
                "overall_confidence": 0.82,
                "model_margin_confidence": 0.85,
                "data_completeness_score": 0.95,
                "spatial_coverage_confidence": 0.70
            },
            "evidence": [
                {
                    "category": "DIRECT_OBSERVATION",
                    "source": "CPCB_CAAQMS_GROUND_SENSOR",
                    "metric": "pm25_clean",
                    "value": 158.0,
                    "attribution_note": "Ground PM2.5 measurement (158.0 ug/m3) exceeds threshold by 2.6x."
                },
                {
                    "category": "REMOTE_SENSING",
                    "source": "NASA_FIRMS_VIIRS",
                    "metric": "fire_upwind_alignment_score",
                    "value": 8.4,
                    "attribution_note": "4 thermal anomalies detected upwind within 25km transport corridor."
                },
                {
                    "category": "GIS_CONTEXT",
                    "source": "MUNICIPAL_GIS_MIDC",
                    "metric": "dist_to_nearest_industrial_km",
                    "value": 0.8,
                    "attribution_note": "Location is 0.80 km from designated industrial MIDC boundary."
                }
            ]
        },
        "expected_factual_elements": ["152.0", "168.5", "174.0", "0.88", "886196944dfffff"],
        "expected_consistency": "consistent",
        "requires_non_causal": True
    },
    {
        "id": "CASE_02_CONFLICTING_SIGNALS",
        "description": "Ground station spike, but clean cloud-free satellite column and 0 fires.",
        "f3_payload": {
            "timestamp": "2026-09-25T15:00:00Z",
            "h3_cell_id": "886196944bfffff",
            "hotspot": {
                "calibrated_hotspot_probability": 0.45,
                "operational_threshold": 0.20,
                "is_hotspot": True,
                "governance_claim": "Associated statistical observations only."
            },
            "forecast": {
                "pm25_t_plus_1h": 85.0,
                "pm25_t_plus_3h": 72.0,
                "pm25_t_plus_6h": 58.0,
                "uncertainty_lower_bound_p10": 70.0,
                "uncertainty_upper_bound_p90": 98.0
            },
            "confidence": {
                "overall_confidence": 0.54,
                "model_margin_confidence": 0.45,
                "data_completeness_score": 0.90,
                "spatial_coverage_confidence": 0.40
            },
            "evidence": [
                {
                    "category": "DIRECT_OBSERVATION",
                    "source": "CPCB_CAAQMS_GROUND_SENSOR",
                    "metric": "pm25_clean",
                    "value": 89.0,
                    "attribution_note": "Ground PM2.5 elevated at 89.0 ug/m3."
                },
                {
                    "category": "REMOTE_SENSING",
                    "source": "SENTINEL_5P_TROPOMI",
                    "metric": "satellite_no2_trop",
                    "value": 0.00003,
                    "attribution_note": "Tropospheric column shows no detectable anomaly (nominal baseline)."
                }
            ]
        },
        "expected_factual_elements": ["85.0", "72.0", "58.0", "0.45"],
        "expected_consistency": "conflicting",
        "requires_non_causal": True
    },
    {
        "id": "CASE_03_SPARSE_MONITORING_GAP",
        "description": "Location >12km from nearest reference station; model notes coverage gap uncertainty.",
        "f3_payload": {
            "timestamp": "2026-09-25T16:00:00Z",
            "h3_cell_id": "8861969461fffff",
            "hotspot": {
                "calibrated_hotspot_probability": 0.24,
                "operational_threshold": 0.20,
                "is_hotspot": True,
                "governance_claim": "Associated statistical observations only."
            },
            "forecast": {
                "pm25_t_plus_1h": 64.0,
                "pm25_t_plus_3h": 61.0,
                "pm25_t_plus_6h": 55.0,
                "uncertainty_lower_bound_p10": 48.0,
                "uncertainty_upper_bound_p90": 78.0
            },
            "confidence": {
                "overall_confidence": 0.38,
                "model_margin_confidence": 0.20,
                "data_completeness_score": 0.75,
                "spatial_coverage_confidence": 0.30
            },
            "evidence": [
                {
                    "category": "MONITORING_COVERAGE",
                    "source": "AEROSENTINEL_SPATIAL_INDEX",
                    "metric": "monitoring_coverage_gap_flag",
                    "value": 1,
                    "attribution_note": "Nearest ground monitor is 14.2 km away; elevated uncertainty for ground-level interpolation."
                }
            ]
        },
        "expected_factual_elements": ["64.0", "61.0", "55.0", "0.24"],
        "expected_consistency": "insufficient_evidence",
        "requires_non_causal": True
    }
]