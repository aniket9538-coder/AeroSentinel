# AeroSentinel — Datasets & Reference Geospatial Layers

---

## 1. Primary Ingestion Data Sources

### 1.1 Ground Air Quality Data (CPCB / OpenAQ)
- **Source**: Central Pollution Control Board (India) via OpenAQ REST API and National Air Quality Index portal.
- **Parameters**: PM2.5, PM10, NO2, SO2, CO, O3, Ambient Noise, Temperature.
- **Temporal Resolution**: 15-minute raw telemetry, aggregated into 1-hour means.

### 1.2 Meteorological Data (IMD / OpenWeatherMap)
- **Source**: India Meteorological Department / OpenWeatherMap OneCall API.
- **Parameters**: 2m Temperature, Relative Humidity, 10m Wind Speed, 10m Wind Direction, Atmospheric Pressure, Hourly Precipitation.

### 1.3 NASA FIRMS Active Fire Detections
- **Source**: NASA Fire Information for Resource Management System (VIIRS SNPP, NOAA-20, MODIS).
- **Parameters**: Latitude, Longitude, Brightness Temperature, Fire Radiative Power (FRP), Confidence (0–100%), Detection UTC Timestamp.

### 1.4 Satellite Atmospheric Tropospheric Columns (Sentinel-5P)
- **Source**: European Space Agency Copernicus Sentinel-5P via Google Earth Engine API.
- **Products**: Level 3 Offline Tropospheric NO2 Column Density, SO2 Total Column, UV Aerosol Index (AER AI).

---

## 2. Geospatial Reference Layers (`data/geo/`)

To enrich feature vectors and provide spatial context for incident triage:
- `cities.geojson`: Bounding polygons and geographic center points for pilot cities (Pune, Mumbai, Delhi).
- `roads.geojson`: Arterial highway and major road networks representing vehicular emission corridors.
- `industrial_zones.geojson`: State Industrial Development Corporation (MIDC/GIDC) industrial zones.
- `agricultural_zones.geojson`: Agricultural zones susceptible to seasonal crop stubble burning.
- `schools.geojson`: Sensitive receptor zones (vulnerable pediatric populations).
- `hospitals.geojson`: Sensitive receptor healthcare zones.
- `monitoring_stations.geojson`: Geographic coordinates of official CAAQMS monitoring stations.
