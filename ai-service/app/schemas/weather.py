"""
AeroSentinel - Surface Meteorology Observation Schema
File: ai-service/app/schemas/weather.py

Inherits from EnvironmentalObservation. Enforces meteorological range constraints,
handles calm wind edge cases, and computes orthogonal U/V wind velocity vectors.
"""

import math
from typing import Any, Optional
from pydantic import ConfigDict, Field, field_validator, model_validator
from .canonical import EnvironmentalObservation, QualityFlag


class WeatherObservation(EnvironmentalObservation):
    """
    Standardized observation schema for surface weather (IMD / Open-Meteo / ERA5).
    Preserves raw observations and derives continuous wind vectors needed for dispersion modeling.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "observation_id": "3d5f9a72-8a1e-4c2d-9b6f-7e8a9b0c1d2e",
                "source": "IMD_OPENMETEO",
                "source_record_id": "PUN_WX_2026-09-24T10:00:00Z",
                "observed_at": "2026-09-24T10:00:00+05:30",
                "latitude": 18.5204,
                "longitude": 73.8567,
                "city_id": "city-pune",
                "h3_cell_id": "886196944dfffff",
                "temperature": 28.4,
                "humidity": 65.0,
                "wind_speed": 3.2,
                "wind_direction": 245.0,
                "wind_u": 2.9,
                "wind_v": 1.35,
                "rainfall": 0.0,
                "pressure": 1012.5,
                "data_status": "live",
                "quality_flag": "valid"
            }
        }
    )

    temperature: Optional[float] = Field(
        default=None, 
        ge=-20.0, 
        le=60.0, 
        description="Dry-bulb surface temperature in degrees Celsius (Bound: [-20.0, 60.0])"
    )
    humidity: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=100.0, 
        description="Relative humidity percentage (Bound: [0.0, 100.0])"
    )
    wind_speed: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=100.0, 
        description="Surface horizontal wind speed at 10m height in m/s (Bound: [0.0, 100.0])"
    )
    wind_direction: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=360.0, 
        description="Wind azimuth direction in degrees FROM which wind blows (Bound: [0.0, 360.0])"
    )
    wind_u: Optional[float] = Field(
        default=None,
        ge=-100.0,
        le=100.0,
        description="Derived East-West orthogonal wind component in m/s (Eastward positive)"
    )
    wind_v: Optional[float] = Field(
        default=None,
        ge=-100.0,
        le=100.0,
        description="Derived North-South orthogonal wind component in m/s (Northward positive)"
    )
    rainfall: Optional[float] = Field(
        default=0.0, 
        ge=0.0, 
        le=500.0, 
        description="Precipitation accumulated over the observation hour in mm (Bound: [0.0, 500.0])"
    )
    pressure: Optional[float] = Field(
        default=None, 
        ge=800.0, 
        le=1100.0, 
        description="Atmospheric surface pressure in hPa (Bound: [800.0, 1100.0])"
    )

    @field_validator(
        "temperature", "humidity", "wind_speed", "wind_direction", "rainfall", "pressure", 
        mode="before"
    )
    @classmethod
    def parse_numeric_floats(cls, v: Any) -> Optional[float]:
        """Convert string inputs or nulls cleanly to rounded floats."""
        if v is None or v == "" or (isinstance(v, str) and v.lower() in ["nan", "null", "-999", "-999.0", "na"]):
            return None
        try:
            return round(float(v), 2)
        except (ValueError, TypeError):
            raise ValueError(f"Value must be numeric, got: {v}")

    @model_validator(mode="after")
    def compute_and_validate_wind_components(self) -> "WeatherObservation":
        """
        Derives Cartesian U/V wind components from speed and direction.
        Properly resolves calm wind boundary conditions (< 0.2 m/s).
        """
        # Flag missing core meteorological baseline if critical variables are absent
        if self.temperature is None or self.humidity is None or self.wind_speed is None:
            if self.quality_flag != QualityFlag.INVALID:
                self.quality_flag = QualityFlag.MISSING
            if self.wind_speed is None:
                self.wind_u = None
                self.wind_v = None
                return self

        # Calm conditions: speed < 0.2 m/s
        if self.wind_speed < 0.2:
            self.wind_u = 0.0
            self.wind_v = 0.0
            if self.wind_direction is None:
                self.wind_direction = 0.0
            return self

        # Non-calm conditions with missing direction
        if self.wind_direction is None:
            self.wind_u = 0.0
            self.wind_v = 0.0
            self.quality_flag = QualityFlag.SUSPECT
            return self

        # Meteorological decomposition:
        # Direction represents angle FROM which wind blows.
        # Vector pointing direction: U = -ws * sin(rad), V = -ws * cos(rad)
        rad = math.radians(self.wind_direction)
        self.wind_u = round(float(-self.wind_speed * math.sin(rad)), 4)
        self.wind_v = round(float(-self.wind_speed * math.cos(rad)), 4)

        return self