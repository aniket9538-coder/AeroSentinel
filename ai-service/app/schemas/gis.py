"""
AeroSentinel - GIS Reference Feature Schema
File: ai-service/app/schemas/gis.py

Standardizes vector geometries, administrative boundaries, monitoring stations,
and sensitive urban receptors.
"""

from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field, field_validator
import h3


def _is_valid_h3_cell(cell_str: str) -> bool:
    """Helper ensuring compatibility across both H3 v3 and H3 v4 bindings."""
    if hasattr(h3, "is_valid_cell"):
        return h3.is_valid_cell(cell_str)
    elif hasattr(h3, "h3_is_valid"):
        return h3.h3_is_valid(cell_str)
    return False


def _get_h3_resolution(cell_str: str) -> int:
    """Helper getting cell resolution across H3 v3 and H3 v4."""
    if hasattr(h3, "get_resolution"):
        return h3.get_resolution(cell_str)
    elif hasattr(h3, "h3_get_resolution"):
        return h3.h3_get_resolution(cell_str)
    return -1


class GISFeatureType(str, Enum):
    """Classification categories for vector reference layers."""
    STATION = "STATION"
    BOUNDARY = "BOUNDARY"
    HOTSPOT = "HOTSPOT"
    ROAD_NETWORK = "ROAD_NETWORK"
    INDUSTRIAL_ZONE = "INDUSTRIAL_ZONE"
    AGRICULTURAL_ZONE = "AGRICULTURAL_ZONE"
    SCHOOL = "SCHOOL"
    HOSPITAL = "HOSPITAL"


class GISReferenceFeature(BaseModel):
    """Standardized GIS reference vector layer schema."""

    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "feature_id": "ST-PUN-001",
                "feature_type": "STATION",
                "name": "Shivajinagar CAAQMS",
                "geometry_type": "Point",
                "coordinates": [73.8567, 18.5204],
                "h3_res8": "886196944dfffff",
                "properties": {
                    "city_id": "city-pune",
                    "operator": "MPCB"
                }
            }
        }
    )

    feature_id: str = Field(
        ...,
        min_length=2,
        max_length=64,
        description="Unique identifier for the GIS feature"
    )
    feature_type: GISFeatureType = Field(
        ...,
        description="Classification category of the geographic feature"
    )
    name: str = Field(
        ...,
        max_length=128,
        description="Human-readable name or label"
    )
    geometry_type: str = Field(
        default="Point",
        description="GeoJSON geometry primitive type (Point, LineString, Polygon, MultiPolygon)"
    )
    coordinates: List[Any] = Field(
        ...,
        description="GeoJSON coordinates array [lon, lat] for Point or nested coordinate arrays"
    )
    h3_res8: Optional[str] = Field(
        default=None,
        min_length=15,
        max_length=15,
        description="Canonical 15-character lowercase hexadecimal H3 spatial index at Resolution 8"
    )
    properties: Dict[str, Any] = Field(
        default_factory=dict,
        description="Arbitrary feature attributes and metadata payload"
    )

    @field_validator("feature_type", mode="before")
    @classmethod
    def normalize_feature_type(cls, v: Any) -> Any:
        """Allow case-insensitive parsing of feature types."""
        if isinstance(v, str):
            clean_str = v.strip().upper()
            try:
                return GISFeatureType(clean_str)
            except ValueError:
                return clean_str
        return v

    @field_validator("feature_id", "name", mode="before")
    @classmethod
    def sanitize_strings(cls, v: Optional[str]) -> Optional[str]:
        """Strip leading/trailing whitespace from identifiers and labels."""
        return v.strip() if isinstance(v, str) else v

    @field_validator("h3_res8")
    @classmethod
    def validate_h3_res8(cls, v: Optional[str]) -> Optional[str]:
        """Validate H3 index format and ensure Resolution 8 conformance."""
        if v is not None:
            clean_v = v.strip().lower()
            if not _is_valid_h3_cell(clean_v):
                raise ValueError(f"Invalid H3 index string: '{v}'. Must be a valid 15-character hex cell ID.")

            res = _get_h3_resolution(clean_v)
            if res != 8:
                raise ValueError(
                    f"H3 cell '{clean_v}' has resolution {res}. AeroSentinel standard is strictly Resolution 8."
                )
            return clean_v
        return v