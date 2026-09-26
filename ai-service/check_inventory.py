from pathlib import Path
import yaml

inventory_path = Path(r"C:\Users\Harsh\AeroSentinel\data\dataset_inventory.yaml")

if not inventory_path.exists():
    print(f"[FAIL] File not found at: {inventory_path}")
    exit(1)

with open(inventory_path, "r", encoding="utf-8") as f:
    data = yaml.safe_load(f)

datasets = data.get("datasets", [])
categories = set(d.get("category") for d in datasets)

print("=" * 60)
print(f"AeroSentinel - Inventory Verification (F0-B)")
print("=" * 60)
print(f"[PASS] Successfully loaded {len(datasets)} datasets from {inventory_path.name}")
print(f"[PASS] Documented categories ({len(categories)}):")
for cat in sorted(categories):
    print(f"  - {cat}")
print("=" * 60)
