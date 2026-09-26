"""
AeroSentinel - Spatial Alignment & H3 Grid Binding
Validates WGS84 coordinates and resolves canonical H3 resolution 8 indices.
Compatible with both h3-py v3 and v4 APIs.
"""
from typing import Dict, Optional, Tuple
import h3
import pandas as pd

# Municipal Bounding Boxes
PUNE_BBOX: Dict[str, float] = {
    "min_lat": 18.3000,
    "max_lat": 18.7500,
    "min_lon": 73.6500,
    "max_lon": 74.1500,
}

MUMBAI_BBOX: Dict[str, float] = {
    "min_lat": 18.8500,
    "max_lat": 19.3500,
    "min_lon": 72.7500,
    "max_lon": 73.0500,
}


def _to_h3_cell(lat: float, lon: float, resolution: int) -> str:
    """Internal adapter handling both h3 v3 and v4 functions."""
    if hasattr(h3, "latlng_to_cell"):
        return h3.latlng_to_cell(lat, lon, resolution)
    return h3.geo_to_h3(lat, lon, resolution)


def _is_h3_valid(cell_id: str) -> bool:
    """Internal adapter handling both h3 v3 and v4 validation."""
    if hasattr(h3, "is_valid_cell"):
        return h3.is_valid_cell(cell_id)
    return h3.h3_is_valid(cell_id)


def validate_coordinates(
    lat: float, 
    lon: float, 
    bbox: Optional[Dict[str, float]] = PUNE_BBOX
) -> Tuple[bool, str]:
    """
    Verifies latitude/longitude physically and geographically within defined bounds.
    Pass bbox=None to skip regional municipal bounding checks.
    """
    if pd.isna(lat) or pd.isna(lon):
        return False, "MISSING_COORDINATES"

    try:
        lat = float(lat)
        lon = float(lon)
    except (ValueError, TypeError):
        return False, "INVALID_COORDINATE_FORMAT"

    # WGS84 Physical Limits
    if not (-90.0 <= lat <= 90.0 and -180.0 <= lon <= 180.0):
        return False, "PHYSICALLY_IMPOSSIBLE_COORDINATES"

    # Municipal Regional Boundary Check
    if bbox is not None:
        if not (bbox["min_lat"] <= lat <= bbox["max_lat"] and bbox["min_lon"] <= lon <= bbox["max_lon"]):
            return False, "OUT_OF_BOUNDS_COORDINATES"

    return True, "VALID"


def bind_h3_cell(
    lat: float, 
    lon: float, 
    resolution: int = 8,
    bbox: Optional[Dict[str, float]] = None
) -> Optional[str]:
    """
    Converts coordinate point into canonical H3 hexadecimal cell index.
    Defaults to global validation (bbox=None) unless bbox is explicitly passed.
    """
    valid, _ = validate_coordinates(lat, lon, bbox=bbox)
    if not valid:
        return None
    try:
        cell_id = _to_h3_cell(lat, lon, resolution)
        return str(cell_id) if _is_h3_valid(cell_id) else None
    except Exception:
        return None


def bind_h3_dataframe(
    df: pd.DataFrame,
    lat_col: str = "latitude",
    lon_col: str = "longitude",
    h3_col: str = "h3_res8",
    resolution: int = 8,
    bbox: Optional[Dict[str, float]] = None
) -> pd.DataFrame:
    """
    Applies H3 spatial index mapping across an entire pandas DataFrame.
    """
    df = df.copy()
    df[h3_col] = [
        bind_h3_cell(lat, lon, resolution=resolution, bbox=bbox)
        for lat, lon in zip(df[lat_col], df[lon_col])
    ]
    return df