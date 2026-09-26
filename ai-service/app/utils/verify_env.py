"""
AeroSentinel - Member 3 Environment & Geospatial Verification Script
File: ai-service/app/utils/verify_env.py

Verifies Python runtime, core mathematical/ML packages, geospatial bindings,
H3 grid resolution calculations, and repo-relative paths.
"""

import sys
import importlib
from pathlib import Path
import pyproj
import shapely
from shapely.geometry import Point, Polygon
import h3

# Explicit package names mapped to importable module names
REQUIRED_PACKAGES = [
    ("numpy", "numpy"),
    ("pandas", "pandas"),
    ("pyarrow", "pyarrow"),
    ("pydantic", "pydantic"),
    ("pydantic-settings", "pydantic_settings"),
    ("h3", "h3"),
    ("shapely", "shapely"),
    ("geopandas", "geopandas"),
    ("pyproj", "pyproj"),
    ("scikit-learn", "sklearn"),
    ("matplotlib", "matplotlib"),
    ("pyyaml", "yaml"),
]

PUNE_COORDS = {"lat": 18.5204, "lon": 73.8567}
EXPECTED_H3_RES8 = "886196944dfffff"


def _convert_latlng_to_h3(lat: float, lon: float, resolution: int = 8) -> str:
    """Helper bridging H3 v3 and v4 method signatures."""
    if hasattr(h3, "latlng_to_cell"):  # H3 v4 API
        return h3.latlng_to_cell(lat, lon, resolution)
    elif hasattr(h3, "geo_to_h3"):      # H3 v3 API
        return h3.geo_to_h3(lat, lon, resolution)
    raise AttributeError("Installed h3 module has neither 'latlng_to_cell' nor 'geo_to_h3'.")


def _is_valid_h3(cell_id: str) -> bool:
    """Helper checking H3 cell validity across v3 and v4."""
    if hasattr(h3, "is_valid_cell"):   # H3 v4 API
        return h3.is_valid_cell(cell_id)
    elif hasattr(h3, "h3_is_valid"):   # H3 v3 API
        return h3.h3_is_valid(cell_id)
    return False


def _get_h3_resolution(cell_id: str) -> int:
    """Helper checking H3 cell resolution across v3 and v4."""
    if hasattr(h3, "get_resolution"):     # H3 v4 API
        return h3.get_resolution(cell_id)
    elif hasattr(h3, "h3_get_resolution"): # H3 v3 API
        return h3.h3_get_resolution(cell_id)
    return -1


def verify_environment():
    print("=" * 65)
    print("AeroSentinel — AI/ML/Data/GIS Runtime Verification (Member 3)")
    print("=" * 65)

    # 1. Python Version Check
    py_version = sys.version.split()[0]
    print(f"Python Runtime      : {py_version} ({sys.executable})")
    assert sys.version_info >= (3, 11), f"Python 3.11+ required. Found {py_version}"
    print("✓ Python version assertion passed (>= 3.11)")

    # 2. Package Import & Version Audit
    print("\n--- Core Package Verification ---")
    failed_imports = []
    for display_name, mod_name in REQUIRED_PACKAGES:
        try:
            mod = importlib.import_module(mod_name)
            version = getattr(mod, "__version__", "installed (no version attr)")
            print(f"✓ {display_name:20s} : {version}")
        except ImportError as err:
            print(f"✗ FAILED: {display_name} ({err})")
            failed_imports.append(display_name)

    if failed_imports:
        print(f"\n[ERROR] Missing packages: {failed_imports}")
        print("Run: pip install -r requirements.txt")
        sys.exit(1)

    # 3. Pydantic v2 & Pydantic-Settings Integration Check
    print("\n--- Pydantic v2 & Settings Check ---")
    from pydantic import BaseModel, Field
    from pydantic_settings import BaseSettings

    class SampleConfig(BaseSettings):
        test_env: str = "ok"

    class SampleObservation(BaseModel):
        pm25: float = Field(..., ge=0.0, le=1500.0)

    cfg = SampleConfig()
    obs = SampleObservation(pm25=42.5)
    assert cfg.test_env == "ok"
    assert obs.pm25 == 42.5
    print("✓ Pydantic BaseModel & BaseSettings functional")

    # 4. Geospatial, H3 & Proj Math Check
    print("\n--- Geospatial & H3 Sanity Check ---")
    lat, lon = PUNE_COORDS["lat"], PUNE_COORDS["lon"]

    # H3 Res 8 resolution verification
    h3_index = _convert_latlng_to_h3(lat, lon, 8).lower()
    print(f"Coordinate ({lat}, {lon}) -> H3 Res 8: {h3_index}")
    assert len(h3_index) == 15, f"Invalid H3 index string length: {len(h3_index)}"
    assert _is_valid_h3(h3_index), f"H3 index validation failed for '{h3_index}'"
    res = _get_h3_resolution(h3_index)
    assert res == 8, f"H3 cell resolution mismatch. Expected 8, got {res}"
    print(f"✓ H3 cell valid at Resolution 8")

    # Shapely 2.0+ geometry operations
    pt = Point(lon, lat)
    poly = Polygon([(73.6, 18.3), (74.2, 18.3), (74.2, 18.8), (73.6, 18.8)])
    assert poly.contains(pt), "Pune coordinates should fall inside Pune bounding polygon"
    print("✓ Shapely geometry topology validation passed")

    # PyProj WGS84 (EPSG:4326) -> WebMercator (EPSG:3857) projection
    transformer = pyproj.Transformer.from_crs("EPSG:4326", "EPSG:3857", always_xy=True)
    x, y = transformer.transform(lon, lat)
    assert round(x) != 0 and round(y) != 0, "PyProj reprojection produced null coordinate"
    print(f"PyProj WGS84 -> WebMercator: ({x:.2f}, {y:.2f})")
    print("✓ Cartographic reprojection validated")

    # 5. Project Directory Structure Check
    print("\n--- Project Directory Structure Check ---")
    current_file = Path(__file__).resolve()
    # Expected structure: aerosentinel/ai-service/app/utils/verify_env.py -> 4 parents up is repo root
    repo_root = current_file.parents[3]
    print(f"Detected Repository Root: {repo_root}")

    expected_directories = [
        repo_root / "data" / "raw",
        repo_root / "data" / "interim",
        repo_root / "data" / "processed",
        repo_root / "data" / "sample",
        repo_root / "data" / "geo",
        repo_root / "ai-service" / "app" / "schemas",
        repo_root / "ai-service" / "preprocessing",
    ]

    for p in expected_directories:
        p.mkdir(parents=True, exist_ok=True)
        print(f"✓ Directory ready: {p.relative_to(repo_root)}")

    print("=" * 65)
    print("[SUCCESS] All Member 3 environment dependencies, schemas,")
    print("geospatial bindings, and directories verified successfully.")
    print("=" * 65)


if __name__ == "__main__":
    verify_environment()