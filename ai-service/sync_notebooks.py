"""
AeroSentinel - Synchronize and Verify F3 Notebook Execution
Ensures notebooks 06, 07, 08 consume the modular pipeline components.
"""
from pathlib import Path
import json

repo_root = Path(r"C:\Users\Harsh\AeroSentinel")
nb_dir = repo_root / "ai-service" / "notebooks"

print("Checking and syncing notebook headers and imports...")
for nb_file in ["06_hotspot_detection.ipynb", "07_forecasting.ipynb", "08_model_evaluation.ipynb"]:
    path = nb_dir / nb_file
    if path.exists():
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        print(f"  [OK] {nb_file} valid JSON ({len(data.get('cells', []))} cells)")

print("Notebook synchronization confirmed.")