"""
AeroSentinel - Citizen Hyperlocal Observation Schema
File: ai-service/app/schemas/citizen.py

Inherits from EnvironmentalObservation. Standardizes crowdsourced incident reports,
category classifications, and multimodal media references for Gemini Vision analysis.
"""

from enum import Enum
from typing import Any, Optional
from pydantic import ConfigDict, Field, field_validator
from .canonical import EnvironmentalObservation


class CitizenCategory(str, Enum):
    """Standard incident classifications for crowdsourced pollution reports."""
    SMOKE = "smoke"
    GARBAGE_BURNING = "garbage_burning"
    DUST = "dust"
    INDUSTRIAL = "industrial"
    TRAFFIC = "traffic"
    OTHER = "other"


class CitizenReport(EnvironmentalObservation):
    """
    Standardized schema for citizen-submitted ground reports.
    Provides verified multimodal references and qualitative ground evidence.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        use_enum_values=True,
        validate_assignment=True,
        extra="forbid",
        json_schema_extra={
            "example": {
                "observation_id": "5c1deb4d-1b7d-4bad-9bdd-2b0d7b3dcb6f",
                "source": "CITIZEN_PORTAL",
                "source_record_id": "CR_PUN_2026_00124",
                "observed_at": "2026-09-24T09:15:00+05:30",
                "latitude": 18.5304,
                "longitude": 73.8467,
                "city_id": "city-pune",
                "h3_cell_id": "886196944dfffff",
                "report_id": "CR_PUN_2026_00124",
                "category": "garbage_burning",
                "description": "Dense black smoke emerging from open waste burning near bridge.",
                "image_ref": "https://storage.aerosentinel.org/citizen-reports/2026/09/24/img_00124.jpg",
                "device_accuracy_m": 8.5,
                "data_status": "live",
                "quality_flag": "valid"
            }
        }
    )

    report_id: str = Field(
        ..., 
        min_length=2,
        max_length=64,
        description="Unique identifier for the citizen report record"
    )
    category: CitizenCategory = Field(
        ..., 
        description="Standardized category of observed pollution event"
    )
    description: Optional[str] = Field(
        default=None, 
        max_length=500, 
        description="Optional qualitative textual description submitted by the citizen"
    )
    image_ref: Optional[str] = Field(
        default=None, 
        max_length=512,
        description="Storage URI or public URL to citizen-uploaded photographic evidence"
    )
    device_accuracy_m: Optional[float] = Field(
        default=None, 
        ge=0.0, 
        le=500.0, 
        description="Reported GPS horizontal precision radius in meters (Bound: [0.0, 500.0])"
    )

    @field_validator("report_id", "description", "image_ref", mode="before")
    @classmethod
    def sanitize_strings(cls, v: Optional[str]) -> Optional[str]:
        """Strip surrounding whitespace from string fields."""
        return v.strip() if isinstance(v, str) else v

    @field_validator("category", mode="before")
    @classmethod
    def normalize_category(cls, v: Any) -> Any:
        """Allow case-insensitive input string mapping for CitizenCategory."""
        if isinstance(v, str):
            clean_str = v.strip().lower()
            try:
                return CitizenCategory(clean_str)
            except ValueError:
                return clean_str
        return v

    @field_validator("device_accuracy_m", mode="before")
    @classmethod
    def parse_device_accuracy(cls, v: Any) -> Optional[float]:
        """Parse GPS accuracy radius float."""
        if v is None or v == "" or (isinstance(v, str) and v.lower() in ["nan", "null"]):
            return None
        try:
            return round(float(v), 2)
        except (ValueError, TypeError):
            raise ValueError(f"device_accuracy_m must be numeric, got: {v}")