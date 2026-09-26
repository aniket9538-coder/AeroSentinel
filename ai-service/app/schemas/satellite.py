"""
AeroSentinel - Satellite Atmospheric Column Schema
File: ai-service/app/schemas/satellite.py

Inherits from EnvironmentalObservation. Enforces vertical column density bounds,
pixel-level cloud filtering assertions, and sensor quality assurance flags.
"""

from typing import Any, Optional
from pydantic import ConfigDict, Field, field_validator, model_validator
from .canonical import EnvironmentalObservation, QualityFlag


class SatelliteAtmospheric(EnvironmentalObservation):
    """
    Standardized observation schema for Sentinel-5P TROPOMI Level-2/3 atmospheric products.
    Preserves tropospheric vertical column densities and manages cloud fraction masking.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "observation_id": "7b2eeb4d-2b7d-4bad-9bdd-2b0d7b3dcb6e",
                "source": "SENTINEL_5P",
                "source_record_id": "S5P_RPRO_L2__NO2____20260924T080000",
                "observed_at": "2026-09-24T08:00:00Z",
                "latitude": 18.5204,
                "longitude": 73.8567,
                "city_id": "city-pune",
                "h3_cell_id": "886196944dfffff",
                "tropospheric_no2": 0.00012,
                "so2_column": 0.00005,
                "co_column": 0.035,
                "aerosol_index": 1.45,
                "cloud_fraction": 0.15,
                "data_status": "historical",
                "quality_flag": "VALID"
            }
        }
    )

    tropospheric_no2: Optional[float] = Field(
        default=None, 
        ge=-0.0001, 
        le=0.005, 
        description="Tropospheric vertical column NO2 in mol/m2 (Physical bound: [-0.0001, 0.005])"
    )
    so2_column: Optional[float] = Field(
        default=None, 
        ge=-0.0001, 
        le=0.005, 
        description="Total vertical column SO2 in mol/m2 (Physical bound: [-0.0001, 0.005])"
    )
    co_column: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=0.1, 
        description="Total vertical column CO in mol/m2 (Physical bound: [0.0, 0.1])"
    )
    aerosol_index: Optional[float] = Field(
        default=None, 
        ge=-5.0, 
        le=15.0, 
        description="Absorbing Aerosol Index (UVAI 354/388 nm, Bound: [-5.0, 15.0])"
    )
    cloud_fraction: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=1.0, 
        description="Effective cloud fraction in pixel [0.0, 1.0] (Used for QA filtering > 0.30)"
    )

    @field_validator("tropospheric_no2", "so2_column", "co_column", "aerosol_index", "cloud_fraction", mode="before")
    @classmethod
    def parse_scientific_floats(cls, v: Any) -> Optional[float]:
        """Convert scientific notation strings and handle null representations."""
        if v is None or v == "" or (isinstance(v, str) and v.lower() in ["nan", "null", "_fillvalue", "na"]):
            return None
        try:
            return float(v)
        except (ValueError, TypeError):
            raise ValueError(f"Satellite column value must be numeric, got: {v}")

    @model_validator(mode="after")
    def validate_cloud_quality_filtering(self) -> "SatelliteAtmospheric":
        """
        Flags observations compromised by high cloud cover (> 0.30) as SUSPECT
        to prevent optical column artifacts from contaminating ground models.
        """
        if self.cloud_fraction is not None and self.cloud_fraction > 0.30:
            self.quality_flag = QualityFlag.SUSPECT

        # Mark missing if all atmospheric columns are absent
        if (
            self.tropospheric_no2 is None 
            and self.so2_column is None 
            and self.co_column is None 
            and self.aerosol_index is None
        ):
            self.quality_flag = QualityFlag.MISSING

        return self