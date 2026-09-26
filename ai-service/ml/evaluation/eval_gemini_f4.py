"""
AeroSentinel - Quantitative F4 LLM Evaluation Benchmark
File: ai-service/ml/evaluation/eval_gemini_f4.py
Fulfills Section 35 (Gemini Evaluation) & Section 36 (Evidence Grounding Test).
Measures:
  - Schema validity rate (%)
  - Evidence grounding rate (%)
  - Unsupported / causal claim rate (%)
  - Required-field completeness (%)
  - Consistency classification rate (%)
"""

import sys
from pathlib import Path

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
ai_service_dir = repo_root / "ai-service"
if str(ai_service_dir) not in sys.path:
    sys.path.insert(0, str(ai_service_dir))

from tests.ai.evaluation_dataset import CURATED_F4_EVALUATION_CASES
from app.services.gemini_pipeline import AeroSentinelGeminiPipeline
from app.services.grounding_guard import GroundingValidator


def run_f4_evaluation_benchmark():
    print("=" * 70)
    print("AEROSENTINEL F4: QUANTITATIVE GEMINI BENCHMARK EVALUATION")
    print("=" * 70)

    total_cases = len(CURATED_F4_EVALUATION_CASES)
    schema_valid_count = 0
    grounded_count = 0
    zero_causal_violations_count = 0
    consistency_correct_count = 0
    total_expected_entities = 0
    matched_expected_entities = 0

    for idx, case in enumerate(CURATED_F4_EVALUATION_CASES, 1):
        print(f"\nEvaluating Case {idx}/{total_cases}: {case['id']}...")
        payload = case["f3_payload"]

        # 1. Synthesize explanation
        try:
            explanation = AeroSentinelGeminiPipeline.generate_explanation(payload)
            schema_valid_count += 1
            print("  • Schema Validation: PASSED")
        except Exception as e:
            print(f"  • Schema Validation: FAILED ({e})")
            continue

        # 2. Run Grounding Guard
        grounding_result = GroundingValidator.validate_grounding(payload, explanation)
        if grounding_result.is_grounded:
            grounded_count += 1
            print("  • Grounding Check: PASSED (100% Grounded)")
        else:
            print(f"  • Grounding Check: FAILED ({grounding_result.unsupported_claims})")

        # 3. Check Non-Causal Compliance
        has_causal_leak = (
            explanation.causal_claim_supported or
            any("caus" in c.lower() for c in grounding_result.unsupported_claims)
        )
        if not has_causal_leak:
            zero_causal_violations_count += 1
            print("  • Non-Causal Governance: 100% COMPLIANT")
        else:
            print("  • Non-Causal Governance: VIOLATION DETECTED")

        # 4. Check Consistency Identification
        pred_consistency = explanation.evidence_summary.evidence_consistency
        expected_consistency = case["expected_consistency"]
        if pred_consistency == expected_consistency or (expected_consistency == "conflicting" and pred_consistency in ["conflicting", "partially_consistent"]):
            consistency_correct_count += 1
            print(f"  • Consistency Assessment: MATCHED ({pred_consistency})")
        else:
            print(f"  • Consistency Assessment: DIVERGED (Got: {pred_consistency}, Expected: {expected_consistency})")

        # 5. Measure Numerical Fact Grounding
        full_text = f"{explanation.event_summary_public} {explanation.event_summary_analyst} {explanation.forecast_trajectory}"
        for fact in case["expected_factual_elements"]:
            total_expected_entities += 1
            if fact in full_text:
                matched_expected_entities += 1

    schema_rate = (schema_valid_count / total_cases) * 100
    grounding_rate = (grounded_count / total_cases) * 100
    non_causal_rate = (zero_causal_violations_count / total_cases) * 100
    fact_coverage_rate = (matched_expected_entities / total_expected_entities) * 100 if total_expected_entities > 0 else 100
    consistency_rate = (consistency_correct_count / total_cases) * 100

    print("\n" + "=" * 70)
    print("BENCHMARK SUMMARY RESULTS")
    print("=" * 70)
    print(f"Total Test Cases Evaluated:       {total_cases}")
    print(f"Schema Validity Rate:             {schema_rate:.1f}%")
    print(f"Evidence Grounding Rate:          {grounding_rate:.1f}%")
    print(f"Non-Causal Compliance Rate:       {non_causal_rate:.1f}%")
    print(f"Numerical Fact Extraction Rate:   {fact_coverage_rate:.1f}%")
    print(f"Consistency Assessment Rate:      {consistency_rate:.1f}%")
    print("=" * 70)

    return {
        "schema_validity_rate": schema_rate,
        "evidence_grounding_rate": grounding_rate,
        "non_causal_compliance_rate": non_causal_rate,
        "fact_extraction_rate": fact_coverage_rate,
        "consistency_rate": consistency_rate
    }


if __name__ == "__main__":
    run_f4_evaluation_benchmark()