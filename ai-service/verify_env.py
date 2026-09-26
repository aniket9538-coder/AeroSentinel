"""
AeroSentinel - F0 Environment Verification Script
Validates Python version and core Data, GIS, and ML dependencies.
"""
import sys

def verify_environment():
    print("=" * 60)
    print("AeroSentinel - Environment Verification (F0-A)")
    print("=" * 60)
    
    # 1. Python Version Check
    py_ver = sys.version_info
    print(f"Python Runtime: {sys.version.split()[0]}")
    assert py_ver.major == 3 and py_ver.minor == 11, f"Expected Python 3.11, found {py_ver.major}.{py_ver.minor}"
    print("  [PASS] Python 3.11 target matched.")

    # 2. Data Libraries
    import pandas as pd
    import numpy as np
    import pydantic
    import pyarrow
    import yaml
    print(f"  [PASS] pandas: {pd.__version__}")
    print(f"  [PASS] numpy: {np.__version__}")
    print(f"  [PASS] pydantic: {pydantic.__version__}")
    print(f"  [PASS] pyarrow: {pyarrow.__version__}")
    print(f"  [PASS] pyyaml: {yaml.__version__}")

    # 3. GIS & Spatial Libraries
    import shapely
    import geopandas as gpd
    import pyproj
    import h3
    print(f"  [PASS] shapely: {shapely.__version__}")
    print(f"  [PASS] geopandas: {gpd.__version__}")
    print(f"  [PASS] pyproj: {pyproj.__version__}")
    print(f"  [PASS] h3: {h3.__version__}")
    
    # Test H3 binding
    test_lat, test_lon = 19.0760, 72.8777
    h3_func = getattr(h3, "latlng_to_cell", getattr(h3, "geo_to_h3", None))
    assert h3_func is not None, "H3 indexing function not found"
    cell = h3_func(test_lat, test_lon, 8)
    print(f"  [PASS] H3 test cell resolution 8 (Mumbai BKC): {cell}")

    # 4. Machine Learning & Visualization
    import sklearn
    import matplotlib
    print(f"  [PASS] scikit-learn: {sklearn.__version__}")
    print(f"  [PASS] matplotlib: {matplotlib.__version__}")
    
    print("=" * 60)
    print("ALL CORE DEPENDENCIES VERIFIED SUCCESSFULLY.")
    print("=" * 60)

if __name__ == "__main__":
    verify_environment()