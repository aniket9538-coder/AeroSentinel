"""
AeroSentinel - Ground Air Quality Observation Schema
File: ai-service/app/schemas/air_quality.py

Inherits from EnvironmentalObservation. Enforces physical concentration bounds,
cross-pollutant plausibility, and automated data quality flagging.
"""

from typing import Any, Optional
from pydantic import ConfigDict, Field, field_validator, model_validator
from .canonical import EnvironmentalObservation, QualityFlag


class AirQualityObservation(EnvironmentalObservation):
    """
    Standardized observation schema for ground monitoring stations (CPCB / CAAQMS / OpenAQ).
    Preserves raw measurements, applies physical range assertions, and classifies sensor health.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "observation_id": "8c3b28b7-4f67-4e31-8f92-5d9c72e2cf61",
                "source": "CPCB",
                "source_record_id": "ST-PUN-001_2026-09-24T10:00:00Z",
                "observed_at": "2026-09-24T10:00:00+05:30",
                "station_id": "ST-PUN-001",
                "station_name": "Shivajinagar",
                "latitude": 18.5204,
                "longitude": 73.8567,
                "city_id": "city-pune",
                "h3_cell_id": "886196944dfffff",
                "pm25": 84.5,
                "pm10": 142.0,
                "no2": 32.4,
                "so2": 14.1,
                "co": 1.2,
                "o3": 28.0,
                "aqi": 182,
                "data_status": "live",
                "quality_flag": "valid"
            }
        }
    )

    station_id: str = Field(
        ..., 
        min_length=2,
        max_length=64,
        description="Unique alphanumeric monitoring station identifier (e.g., ST-PUN-001)"
    )
    station_name: Optional[str] = Field(
        default=None, 
        max_length=128,
        description="Human-readable station label or locality name"
    )
    pm25: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=1500.0, 
        description="Ambient PM2.5 mass concentration in ug/m3 (Physical bound: [0.0, 1500.0])"
    )
    pm10: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=2000.0, 
        description="Ambient PM10 mass concentration in ug/m3 (Physical bound: [0.0, 2000.0])"
    )
    no2: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=1000.0, 
        description="Nitrogen dioxide concentration in ug/m3 (Physical bound: [0.0, 1000.0])"
    )
    so2: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=1000.0, 
        description="Sulfur dioxide concentration in ug/m3 (Physical bound: [0.0, 1000.0])"
    )
    co: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=100.0, 
        description="Carbon monoxide concentration in mg/m3 (Physical bound: [0.0, 100.0])"
    )
    o3: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=1000.0, 
        description="Surface ozone concentration in ug/m3 (Physical bound: [0.0, 1000.0])"
    )
    aqi: Optional[int] = Field(
        default=None, 
        ge=0, 
        le=1000, 
        description="Official sub-index or aggregated AQI if published by station"
    )

    @field_validator("station_id", "station_name", mode="before")
    @classmethod
    def sanitize_strings(cls, v: Optional[str]) -> Optional[str]:
        """Strip surrounding whitespaces from station identifiers."""
        return v.strip() if isinstance(v, str) else v

    @field_validator("pm25", "pm10", "no2", "so2", "co", "o3", mode="before")
    @classmethod
    def validate_pollutant_values(cls, v: Any) -> Optional[float]:
        """Handle nulls, NaNs, and negative analyzer calibration errors."""
        if v is None or v == "" or (isinstance(v, str) and v.lower() in ["nan", "null", "-999", "-999.0", "na"]):
            return None
        try:
            val = float(v)
        except (ValueError, TypeError):
            raise ValueError(f"Pollutant concentration must be numeric, got: {v}")
            
        if val < 0.0:
            raise ValueError(f"Negative concentration ({val}) is physically impossible. Check sensor calibration.")
        return round(val, 2)

    @model_validator(mode="after")
    def validate_cross_pollutant_consistency(self) -> "AirQualityObservation":
        """
        Applies scientific cross-pollutant checks and flags extreme concentration spikes.
        """
        # Flag missing PM2.5 baseline if absent
        if self.pm25 is None:
            self.quality_flag = QualityFlag.MISSING
            return self

        # Physical ratio assertion: PM2.5 cannot exceed total PM10
        if self.pm10 is not None and self.pm25 is not None:
            # If PM2.5 exceeds PM10 by more than 5% instrument tolerance, mark SUSPECT for audit
            if self.pm25 > (self.pm10 * 1.05):
                self.quality_flag = QualityFlag.SUSPECT
                return self

        # Flag severe pollution spikes as SUSPECT rather than clipping or discarding
        if self.pm25 > 500.0:
            self.quality_flag = QualityFlag.SUSPECT
        elif self.quality_flag not in [QualityFlag.SUSPECT, QualityFlag.INVALID, QualityFlag.MISSING]:
            self.quality_flag = QualityFlag.VALID

        return self