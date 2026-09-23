# AeroSentinel — REST API Contract

**Base URL:** `/api/v1`  
**Data Format:** JSON (Requests and Responses)  
**Timestamp Format:** ISO-8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`)  
**Coordinate Reference:** WGS84 (EPSG:4326)  

---

## 1. Authentication & Security

All private endpoints require an `Authorization` header containing a valid Bearer JWT:
```text
Authorization: Bearer <jwt_token>
```

### Standard Error Response Format
```json
{
  "timestamp": "2026-09-23T14:30:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Field 'latitude' must be between -90 and 90",
  "path": "/api/v1/citizen/reports"
}
```

---

## 2. API Endpoints Specification

### 2.1 Public & Environmental Observability

#### `GET /api/v1/cities`
Returns list of supported municipal regions.
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Pune",
    "state": "Maharashtra",
    "country": "India",
    "latitude": 18.5204,
    "longitude": 73.8567,
    "active": true
  }
]
```

#### `GET /api/v1/air/current?cityId={cityId}`
Returns latest CAAQMS air quality observations for a city.
```json
[
  {
    "stationId": "PUN-001",
    "stationName": "Shivajinagar",
    "latitude": 18.5314,
    "longitude": 73.8446,
    "observedAt": "2026-09-23T14:00:00Z",
    "pm25": 84.5,
    "pm10": 142.0,
    "no2": 32.1,
    "so2": 14.8,
    "co": 1.2,
    "aqi": 182,
    "source": "CPCB"
  }
]
```

#### `GET /api/v1/weather/current?cityId={cityId}`
Returns latest meteorological observations.

#### `GET /api/v1/fires?cityId={cityId}&hours=24`
Returns active fire/thermal anomaly points detected by NASA FIRMS.

#### `GET /api/v1/satellite?cityId={cityId}`
Returns latest Sentinel-5P tropospheric column indicators.

---

### 2.2 Spatial & Model Intelligence

#### `GET /api/v1/grid?cityId={cityId}&resolution=8`
Returns active H3 cells and baseline aggregated parameters.

#### `GET /api/v1/hotspots?cityId={cityId}`
Returns potential pollution hotspots identified by the ML pipeline.
```json
[
  {
    "id": "8f2d50860000000",
    "h3Index": "8860144aa1fffff",
    "cityId": "550e8400-e29b-41d4-a716-446655440000",
    "predictedAt": "2026-09-23T14:00:00Z",
    "riskScore": 88.5,
    "riskLevel": "HIGH",
    "confidence": 0.89,
    "primaryFactors": ["Low wind dispersion", "Rising PM2.5 trend", "Active thermal fire 1.2km upwind"],
    "modelVersion": "xgb-hotspot-v1.2"
  }
]
```

#### `GET /api/v1/forecast/{h3Index}`
Returns rolling 1 to 6-hour forecast for a specific H3 hexagonal cell.
```json
{
  "h3Index": "8860144aa1fffff",
  "generatedAt": "2026-09-23T14:00:00Z",
  "unit": "ug/m3",
  "forecast": [
    { "targetHour": 1, "predictedPm25": 92.4, "lowerBound": 81.0, "upperBound": 103.8, "confidence": 0.92 },
    { "targetHour": 2, "predictedPm25": 105.1, "lowerBound": 89.5, "upperBound": 120.7, "confidence": 0.87 },
    { "targetHour": 3, "predictedPm25": 118.0, "lowerBound": 98.2, "upperBound": 137.8, "confidence": 0.81 }
  ]
}
```

---

### 2.3 Citizen Reporting & Multimodal Analysis

#### `POST /api/v1/citizen/reports`
Submits a crowdsourced observation with optional photo.
- Content-Type: `multipart/form-data`
- Fields: `cityId`, `latitude`, `longitude`, `category` (`SMOKE`, `BURNING`, `DUST`, `ODOR`, `OTHER`), `description`, `image` (file).

#### `GET /api/v1/citizen/reports?cityId={cityId}`
Retrieves citizen reports with verification status.

---

### 2.4 Incident Queue & Authority Operations

#### `GET /api/v1/alerts?cityId={cityId}&status=OPEN`
Retrieves pending pollution incident alerts for authority response.

#### `PATCH /api/v1/alerts/{alertId}/acknowledge`
Transitions alert status to `ACKNOWLEDGED`.

#### `POST /api/v1/inspections`
Dispatches a field verification team or mobile sensor crew.

#### `POST /api/v1/actions`
Logs mitigation action taken (e.g., water misting, fire suppression, industrial advisory) and marks incident resolved.

---

### 2.5 Monitoring Coverage & Sensor Recommendations

#### `GET /api/v1/monitoring/recommendations?cityId={cityId}`
Returns ranked H3 cells where mobile sensors should be deployed based on risk, model uncertainty, and distance from fixed stations.
