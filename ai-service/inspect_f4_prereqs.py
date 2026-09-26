"""
AeroSentinel - Step 1-4: Phase 4 Pre-requisite Audit
Inspects F3 contracts, configuration, Gemini SDK availability, and citizen imagery assets.
"""
from pathlib import Path
import os
import sys

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
ai_service_dir = repo_root / "ai-service"
if str(ai_service_dir) not in sys.path:
    sys.path.insert(0, str(ai_service_dir))

print("=" * 70)
print("1. CHECKING F3 SCHEMAS & PRODUCTION MODULES")
print("=" * 70)
try:
    from app.schemas.contracts import AeroSentinelInferencePayload, HotspotAssessment, ForecastHorizons
    from ml.explainability.evidence import generate_structured_evidence, generate_event_payload
    print("[PASS] Successfully imported F3 contracts and evidence generator.")
except Exception as e:
    print(f"[FAIL] Error importing F3 components: {e}")

print("\n" + "=" * 70)
print("2. CHECKING CONFIGURATION & GOOGLE GENAI / GEMINI SDK")
print("=" * 70)
try:
    from app.utils.config import settings
    print(f"Settings loaded. Root: {settings.ROOT_DIR}")
    gemini_key = os.getenv("GEMINI_API_KEY") or getattr(settings, "GEMINI_API_KEY", None)
    if gemini_key:
        masked = gemini_key[:4] + "..." + gemini_key[-4:] if len(gemini_key) > 8 else "***"
        print(f"[INFO] GEMINI_API_KEY detected: {masked}")
    else:
        print("[WARN] GEMINI_API_KEY not currently set in environment or settings.")
except Exception as e:
    print(f"[FAIL] Error inspecting settings: {e}")

try:
    import google.generativeai as genai
    print(f"[PASS] google-generativeai installed. Version: {genai.__version__}")
except ImportError:
    try:
        from google import genai
        print("[PASS] google.genai (V2 SDK) installed.")
    except ImportError:
        print("[FAIL] Neither google-generativeai nor google.genai installed. Will need pip install.")

print("\n" + "=" * 70)
print("3. CHECKING SAMPLE/CITIZEN ASSETS")
print("=" * 70)
sample_dir = repo_root / "data" / "sample"
images = list(sample_dir.glob("*.jpg")) + list(sample_dir.glob("*.png")) + list(sample_dir.glob("*.jpeg"))
print(f"Sample directory: {sample_dir}")
print(f"Found {len(images)} image file(s): {[img.name for img in images]}")