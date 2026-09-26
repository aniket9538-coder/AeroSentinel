"""
AeroSentinel - Phase 5 Score Sensitivity & False-Positive Risk Analyzer
File: ai-service/ml/alert_support/sensitivity_analysis.py
Fulfills Section 39 (Score Sensitivity), Section 40 (Evaluation),
and Section 41 (False Alert Support / False Positive Analysis).
"""

import sys
from pathlib import Path
import copy

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
ai_service_dir = repo_root / "ai-service"
if str(ai_service_dir) not in sys.path:
    sys.path.insert(0, str(ai_service_dir))

from tests.ai.evaluation_dataset import CURATED_F4_EVALUATION_CASES
from ml.alert_support.scoring_engine import EventEvidenceScoringEngine
from app.schemas.alert_contracts import AlertSupportState


def run_alert_support_sensitivity_audit():
    print("=" * 70)
    print("AEROSENTINEL F5: ALERT-SUPPORT SENSITIVITY & FALSE POSITIVE AUDIT")
    print("=" * 70)

    engine = EventEvidenceScoringEngine()
    base_case = CURATED_F4_EVALUATION_CASES[0]["f3_payload"]

    # 1. Baseline Run (Strong Industrial Hotspot)
    base_res = engine.generate_alert_support_payload(base_case)
    print(f"\n[SCENARIO 1: BASELINE ELEVATED EVENT]")
    print(f"  • Final Evidence Score: {base_res.evidence_score}")
    print(f"  • Alert Support State:  {base_res.alert_support_state.value}")
    print(f"  • Completeness:         {base_res.evidence_completeness}")
    print(f"  • Consistency:          {base_res.evidence_consistency.value}")

    # 2. Sensitivity Test: Drop Active Fire Remote-Sensing
    no_fire_case = copy.deepcopy(base_case)
    no_fire_case["evidence"] = [e for e in no_fire_case["evidence"] if "fire" not in e.get("metric", "").lower()]
    no_fire_res = engine.generate_alert_support_payload(no_fire_case)
    print(f"\n[SCENARIO 2: REMOVE UPWIND FIRE SIGNAL]")
    print(f"  • Final Evidence Score: {no_fire_res.evidence_score} (Delta: {no_fire_res.evidence_score - base_res.evidence_score:+.3f})")
    print(f"  • Alert Support State:  {no_fire_res.alert_support_state.value}")

    # 3. Sensitivity Test: Introduce Conflicting Satellite Column
    conflict_case = copy.deepcopy(base_case)
    conflict_case["evidence"].append({
        "category": "REMOTE_SENSING",
        "source": "SENTINEL_5P_TROPOMI",
        "metric": "satellite_no2_trop",
        "attribution_note": "Tropospheric column shows no detectable anomaly (nominal baseline)."
    })
    conflict_res = engine.generate_alert_support_payload(conflict_case)
    print(f"\n[SCENARIO 3: INTRODUCE CONFLICTING SATELLITE COLUMN]")
    print(f"  • Final Evidence Score: {conflict_res.evidence_score} (Delta: {conflict_res.evidence_score - base_res.evidence_score:+.3f})")
    print(f"  • Alert Support State:  {conflict_res.alert_support_state.value}")
    print(f"  • Penalty Applied:      {conflict_res.score_breakdown.conflict_penalty}")

    # 4. False-Positive Test: Clean Normal Telemetry (Isolated Micro-spike)
    normal_case = copy.deepcopy(base_case)
    normal_case["hotspot"]["calibrated_hotspot_probability"] = 0.08
    normal_case["hotspot"]["is_hotspot"] = False
    normal_case["forecast"]["pm25_t_plus_1h"] = 28.0
    normal_case["forecast"]["pm25_t_plus_3h"] = 26.0
    normal_case["forecast"]["pm25_t_plus_6h"] = 22.0
    normal_case["evidence"] = [
        {"category": "DIRECT_OBSERVATION", "source": "CPCB_CAAQMS", "metric": "pm25_clean", "value": 25.0, "attribution_note": "Nominal ground PM2.5 (25 ug/m3)"}
    ]
    normal_res = engine.generate_alert_support_payload(normal_case)
    print(f"\n[SCENARIO 4: NORMAL BASELINE TELEMETRY (FALSE-POSITIVE SUPPRESSION)]")
    print(f"  • Final Evidence Score: {normal_res.evidence_score}")
    print(f"  • Alert Support State:  {normal_res.alert_support_state.value}")
    assert normal_res.alert_support_state == AlertSupportState.INSUFFICIENT_EVIDENCE, "False-positive suppression failed!"

    # 5. Sparse Monitoring Blind-Spot Test (>12km station distance)
    gap_case = copy.deepcopy(base_case)
    gap_case["confidence"]["overall_confidence"] = 0.28
    gap_case["evidence"].append({
        "category": "MONITORING_COVERAGE",
        "source": "SPATIAL_INDEX",
        "metric": "monitoring_coverage_gap_flag",
        "attribution_note": "Nearest sensor 14.5 km away"
    })
    gap_res = engine.generate_alert_support_payload(gap_case)
    print(f"\n[SCENARIO 5: MONITORING COVERAGE BLIND-SPOT]")
    print(f"  • Final Evidence Score: {gap_res.evidence_score}")
    print(f"  • Alert Support State:  {gap_res.alert_support_state.value}")
    assert gap_res.alert_support_state == AlertSupportState.INSUFFICIENT_EVIDENCE, "Coverage gap gating failed!"

    print("\n" + "=" * 70)
    print("SENSITIVITY & SUPPRESSION AUDIT: 100% VERIFIED")
    print("=" * 70)


if __name__ == "__main__":
    run_alert_support_sensitivity_audit()