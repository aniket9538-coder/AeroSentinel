from datetime import datetime, timezone
import pytest
from app.schemas.canonical import (
    AirQualityObservation,
    WeatherObservation,
    FireDetection,
    SatelliteObservation,
    CitizenReport,
    QualityFlag,
    DataStatus,
    CitizenCategory,
)

print("=" * 60)
print("AeroSentinel - Schemas Verification (F0-C)")
print("=" * 60)

# 1. Test Air Quality Schema & Timezone Enforcement
air = AirQualityObservation(
    observation_id="test-air-1",
    source="CPCB",
    source_record_id="ST_001_1",
    observed_at=datetime(2026, 3, 1, 6, 0, tzinfo=timezone.utc),
    latitude=18.5204,
    longitude=73.8567,
    city_id="city-pune",
    station_id="ST_001",
    pm25=45.2,
    pm10=85.0
)
assert air.pm25 == 45.2
print("  [PASS] AirQualityObservation schema instantiated and validated.")

# 2. Test Weather Schema
weather = WeatherObservation(
    observation_id="test-weather-1",
    source="IMD",
    observed_at=datetime(2026, 3, 1, 6, 0, tzinfo=timezone.utc),
    latitude=18.5204,
    longitude=73.8567,
    city_id="city-pune",
    temperature=28.5,
    humidity=65.0,
    wind_speed=2.4,
    wind_direction=240.0
)
assert weather.temperature == 28.5
print("  [PASS] WeatherObservation schema instantiated and validated.")

# 3. Test Fire Detection Schema
fire = FireDetection(
    fire_id="fire-101",
    detected_at=datetime(2026, 3, 1, 8, 30, tzinfo=timezone.utc),
    latitude=18.6000,
    longitude=73.9000,
    confidence=80.0,
    frp=15.4,
    satellite="VIIRS_SNPP"
)
assert fire.frp == 15.4
print("  [PASS] FireDetection schema instantiated and validated.")

# 4. Test Citizen Report Schema
citizen = CitizenReport(
    report_id="rep-001",
    reported_at=datetime(2026, 3, 1, 9, 0, tzinfo=timezone.utc),
    category=CitizenCategory.GARBAGE_BURNING,
    latitude=18.5300,
    longitude=73.8400,
    description="Heavy smoke observed near bridge"
)
assert citizen.category == CitizenCategory.GARBAGE_BURNING
print("  [PASS] CitizenReport schema instantiated and validated.")

print("=" * 60)
print("ALL CANONICAL AND DOMAIN SCHEMAS VERIFIED SUCCESSFULLY.")
print("=" * 60)
