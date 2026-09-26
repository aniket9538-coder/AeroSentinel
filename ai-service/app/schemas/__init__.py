"""
AeroSentinel - Data Schemas Package
File: ai-service/app/schemas/__init__.py

Central export hub for all canonical, domain-specific, and GIS reference
schemas used across data ingestion, preprocessing, and model inference.
"""

from typing import Dict, Type, Union
from pydantic import BaseModel

from .canonical import (
    EnvironmentalObservation,
    QualityFlag,
    DataStatus,
)
from .air_quality import AirQualityObservation
from .weather import WeatherObservation
from .fire import FireDetection
from .satellite import SatelliteAtmospheric
from .citizen import CitizenReport, CitizenCategory
from .gis import GISReferenceFeature, GISFeatureType

# Alias for backward compatibility across modules
BaseEnvironmentalObservation = EnvironmentalObservation

# Domain category lookup registry for dynamic pipeline validation
DomainModelType = Type[Union[EnvironmentalObservation, FireDetection, CitizenReport]]

DOMAIN_SCHEMA_REGISTRY: Dict[str, DomainModelType] = {
    "AIR_QUALITY": AirQualityObservation,
    "AIR QUALITY": AirQualityObservation,
    "WEATHER": WeatherObservation,
    "FIRE": FireDetection,
    "SATELLITE": SatelliteAtmospheric,
    "CITIZEN": CitizenReport,
}


def get_schema_by_category(category: str) -> DomainModelType:
    """
    Returns the corresponding Pydantic schema model for a given dataset inventory category.
    Raises KeyError if the category is unknown or unregistered.
    """
    clean_cat = category.strip().upper().replace(" ", "_")
    if clean_cat not in DOMAIN_SCHEMA_REGISTRY:
        raise KeyError(
            f"Unknown schema category '{category}'. "
            f"Available categories: {sorted(list(set(DOMAIN_SCHEMA_REGISTRY.keys())))}"
        )
    return DOMAIN_SCHEMA_REGISTRY[clean_cat]


__all__ = [
    # Canonical Base Models & Enums
    "EnvironmentalObservation",
    "BaseEnvironmentalObservation",
    "QualityFlag",
    "DataStatus",
    # Domain Observation Models
    "AirQualityObservation",
    "WeatherObservation",
    "FireDetection",
    "SatelliteAtmospheric",
    # Citizen Multimodal Models
    "CitizenReport",
    "CitizenCategory",
    # GIS Reference Models
    "GISReferenceFeature",
    "GISFeatureType",
    # Utilities & Registry
    "DOMAIN_SCHEMA_REGISTRY",
    "get_schema_by_category",
]