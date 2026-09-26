"""
AeroSentinel - Active Fire & Thermal Anomaly Schema
File: ai-service/app/schemas/fire.py

Inherits from EnvironmentalObservation. Enforces confidence intervals,
thermal bounds, and instrument-specific provenance standards for NASA FIRMS.
"""

from typing import Any, Optional
from pydantic import ConfigDict, Field, field_validator
from .canonical import EnvironmentalObservation, QualityFlag


class FireDetection(EnvironmentalObservation):
    """
    Standardized schema for thermal anomalies and active fire detections (NASA FIRMS).
    Captures fire radiative energy, instrument certainty, and brightness temperatures.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "observation_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                "source": "NASA_FIRMS",
                "source_record_id": "FIRMS_VIIRS_2026_09_24_01",
                "observed_at": "2026-09-24T08:30:00Z",
                "latitude": 18.6210,
                "longitude": 73.9120,
                "city_id": "city-pune",
                "h3_cell_id": "886196944dfffff",
                "fire_id": "FIRMS_VIIRS_PUN_001",
                "confidence": 85.0,
                "frp": 14.2,
                "satellite": "VIIRS_SNPP",
                "bright_ti4": 342.5,
                "data_status": "historical",
                "quality_flag": "VALID"
            }
        }
    )

    fire_id: str = Field(
        ..., 
        min_length=2,
        max_length=64,
        description="Unique identifier for detection event from NASA FIRMS"
    )
    confidence: float = Field(
        ..., 
        ge=0.0, 
        le=100.0, 
        description="Detection confidence percentage [0.0, 100.0] or mapped certainty score"
    )
    frp: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=10000.0, 
        description="Fire Radiative Power in Megawatts (MW) (Bound: [0.0, 10000.0])"
    )
    satellite: str = Field(
        ..., 
        min_length=2,
        max_length=64,
        description="Instrument or platform identifier (e.g., VIIRS_SNPP, VIIRS_NOAA20, MODIS_AQUA)"
    )
    bright_ti4: Optional[float] = Field(
        default=None, 
        ge=200.0, 
        le=450.0, 
        description="VIIRS I-4 or MODIS 4um channel brightness temperature in Kelvin (Bound: [200.0, 450.0])"
    )

    @field_validator("fire_id", "satellite", mode="before")
    @classmethod
    def sanitize_strings(cls, v: Optional[str]) -> Optional[str]:
        """Strip surrounding whitespaces from string identifiers."""
        return v.strip() if isinstance(v, str) else v

    @field_validator("confidence", "frp", "bright_ti4", mode="before")
    @classmethod
    def parse_numeric_floats(cls, v: Any) -> Optional[float]:
        """Cleanly handle NaN strings, categorical FIRMS confidences, and float casts."""
        if v is None or v == "" or (isinstance(v, str) and v.lower() in ["nan", "null", "na"]):
            return None
        
        # Map categorical FIRMS labels to standard confidence percentages
        if isinstance(v, str):
            clean_str = v.strip().lower()
            if clean_str in ["l", "low"]:
                return 30.0
            if clean_str in ["n", "nominal"]:
                return 65.0
            if clean_str in ["h", "high"]:
                return 90.0

        try:
            return round(float(v), 2)
        except (ValueError, TypeError):
            raise ValueError(f"Value must be numeric or valid FIRMS category, got: {v}")