"""
AeroSentinel - Step 1-4: Phase 5 Pre-requisite Audit
Inspects F3 and F4 contracts, evidence categories, and existing event structures.
"""
from pathlib import Path
import sys
import json

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
ai_service_dir = repo_root / "ai-service"
if str(ai_service_dir) not in sys.path:
    sys.path.insert(0, str(ai_service_dir))

print("=" * 70)
print("1. CHECKING F3 AND F4 CONTRACT IMPORTS")
print("=" * 70)
try:
    from app.schemas.contracts import AeroSentinelInferencePayload, HotspotAssessment, ForecastHorizons
    from app.schemas.gemini_contracts import StructuredEventExplanation, CitizenVisionAnalysis, EvidenceSummaryOutput
    print("[PASS] Successfully imported F3 and F4 contract schemas.")
except Exception as e:
    print(f"[FAIL] Error importing contracts: {e}")

print("\n" + "=" * 70)
print("2. INSPECTING SAMPLE F3 & F4 DATA SCHEMAS")
print("=" * 70)
try:
    from tests.ai.evaluation_dataset import CURATED_F4_EVALUATION_CASES
    sample_case = CURATED_F4_EVALUATION_CASES[0]
    print(f"[INFO] Sample case ID: {sample_case['id']}")
    print(f"[INFO] Available F3 payload keys: {list(sample_case['f3_payload'].keys())}")
    evidence_types = {e.get('category') or e.get('type') for e in sample_case['f3_payload'].get('evidence', [])}
    print(f"[INFO] Distinct evidence categories present in F3: {evidence_types}")
except Exception as e:
    print(f"[FAIL] Error reading evaluation dataset: {e}")