"""
AeroSentinel - Canonical Data Schema
Defines the base environmental observation contract inherited by all observation domains.
Enforces strict spatial, temporal, provenance, and data hygiene standards.
"""

from datetime import datetime, timezone
from enum import Enum
from typing import Any, Optional
from pydantic import BaseModel, ConfigDict, Field, field_validator
import h3


class QualityFlag(str, Enum):
    """Data hygiene classification flags."""
    VALID = "valid"
    SUSPECT = "suspect"
    INVALID = "invalid"
    MISSING = "missing"


class DataStatus(str, Enum):
    """Operational lifecycle status of the data record."""
    LIVE = "live"
    HISTORICAL = "historical"
    CACHED = "cached"
    SIMULATED = "simulated"
    SYNTHETIC = "synthetic"


def _is_valid_h3_cell(cell_str: str) -> bool:
    """Helper ensuring compatibility across both H3 v3 and H3 v4 bindings."""
    if hasattr(h3, "is_valid_cell"):
        return h3.is_valid_cell(cell_str)  # H3 v4 API
    elif hasattr(h3, "h3_is_valid"):
        return h3.h3_is_valid(cell_str)    # H3 v3 API
    return False


def _get_h3_resolution(cell_str: str) -> int:
    """Helper getting cell resolution across H3 v3 and H3 v4."""
    if hasattr(h3, "get_resolution"):
        return h3.get_resolution(cell_str)  # H3 v4 API
    elif hasattr(h3, "h3_get_resolution"):
        return h3.h3_get_resolution(cell_str)  # H3 v3 API
    return -1


class EnvironmentalObservation(BaseModel):
    """
    Base canonical schema inherited by Air Quality, Weather, Fire, Satellite,
    and Citizen intelligence domains. Guarantees spatial, temporal, and provenance consistency.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "observation_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                "source": "CPCB",
                "source_record_id": "ST-PUN-001_2026-09-24T10:00:00Z",
                "observed_at": "2026-09-24T10:00:00+05:30",
                "latitude": 18.5204,
                "longitude": 73.8567,
                "city_id": "city-pune",
                "h3_cell_id": "886196944dfffff",
                "data_status": "live",
                "quality_flag": "valid"
            }
        }
    )

    observation_id: str = Field(
        ..., 
        description="Unique UUIDv4 string for the observation record"
    )
    source: str = Field(
        ..., 
        min_length=2,
        max_length=64,
        description="Originating data provider or instrument (e.g., CPCB, IMD, NASA_FIRMS, SENTINEL_5P, CITIZEN)"
    )
    source_record_id: Optional[str] = Field(
        default=None, 
        max_length=128,
        description="Raw primary key or unique identifier in the provider's upstream dataset"
    )
    observed_at: datetime = Field(
        ..., 
        description="ISO 8601 timezone-aware observation timestamp recording when the phenomenon occurred"
    )
    latitude: float = Field(
        ..., 
        ge=-90.0, 
        le=90.0, 
        description="WGS 84 Latitude in decimal degrees (EPSG:4326)"
    )
    longitude: float = Field(
        ..., 
        ge=-180.0, 
        le=180.0, 
        description="WGS 84 Longitude in decimal degrees (EPSG:4326)"
    )
    city_id: Optional[str] = Field(
        default=None, 
        max_length=64,
        description="Standardized city identifier for multi-city isolation (e.g., city-pune, city-mumbai)"
    )
    h3_cell_id: Optional[str] = Field(
        default=None, 
        min_length=15,
        max_length=15,
        description="Canonical 15-character lowercase hexadecimal H3 spatial index at Resolution 8"
    )
    data_status: DataStatus = Field(
        default=DataStatus.LIVE, 
        description="Operational data lineage state (live, historical, cached, simulated, synthetic)"
    )
    ingestion_timestamp: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc), 
        description="UTC timestamp recording system ingestion time for provenance and deduplication"
    )
    quality_flag: QualityFlag = Field(
        default=QualityFlag.VALID, 
        description="Data hygiene status (valid, suspect, invalid, missing)"
    )

    @field_validator("source", "city_id", mode="before")
    @classmethod
    def sanitize_strings(cls, v: Optional[str]) -> Optional[str]:
        """Strip surrounding whitespaces from string identifiers."""
        return v.strip() if isinstance(v, str) else v

    @field_validator("quality_flag", mode="before")
    @classmethod
    def normalize_quality_flag(cls, v: Any) -> Any:
        """Allow case-insensitive input string for QualityFlag."""
        if isinstance(v, str):
            clean_str = v.strip().lower()
            try:
                return QualityFlag(clean_str)
            except ValueError:
                return clean_str
        return v

    @field_validator("data_status", mode="before")
    @classmethod
    def normalize_data_status(cls, v: Any) -> Any:
        """Allow case-insensitive input string for DataStatus."""
        if isinstance(v, str):
            clean_str = v.strip().lower()
            try:
                return DataStatus(clean_str)
            except ValueError:
                return clean_str
        return v

    @field_validator("observed_at")
    @classmethod
    def validate_and_convert_utc(cls, v: datetime) -> datetime:
        """
        Enforce timezone awareness to prevent ambiguous naive datetimes and future temporal leakage.
        Deterministically converts all valid inputs to canonical UTC.
        """
        if v.tzinfo is None or v.tzinfo.utcoffset(v) is None:
            raise ValueError(
                "observed_at must be an ISO 8601 timezone-aware datetime with an explicit offset (e.g., +05:30 or Z)."
            )
        return v.astimezone(timezone.utc)

    @field_validator("ingestion_timestamp")
    @classmethod
    def validate_ingestion_timestamp(cls, v: datetime) -> datetime:
        """Ensure ingestion timestamp is strictly in UTC."""
        if v.tzinfo is None or v.tzinfo.utcoffset(v) is None:
            return v.replace(tzinfo=timezone.utc)
        return v.astimezone(timezone.utc)

    @field_validator("latitude", "longitude")
    @classmethod
    def validate_coordinate_precision(cls, v: float) -> float:
        """Round coordinates to 6 decimal places (~0.1m precision)."""
        return round(float(v), 6)

    @field_validator("h3_cell_id")
    @classmethod
    def validate_h3_index(cls, v: Optional[str]) -> Optional[str]:
        """Validate that the string is a valid H3 hexadecimal index and matches configured Resolution 8."""
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


# Backward compatibility alias
BaseEnvironmentalObservation = EnvironmentalObservation