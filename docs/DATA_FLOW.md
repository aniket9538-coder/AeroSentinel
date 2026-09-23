# AeroSentinel — Data Flow & Normalization Pipeline

---

## 1. High-Level Data Flow

```text
External Data Sources
  ├── CPCB / OpenAQ (Station Pollutants: PM2.5, PM10, NO2, SO2, CO)
  ├── IMD / OpenWeather (Weather: Temp, Humidity, Wind Speed & Dir, Rain)
  ├── NASA FIRMS (Thermal Anomalies: Lat, Lon, Brightness, Confidence, FRP)
  ├── Sentinel-5P (Atmospheric Columns: NO2, SO2, Aerosol Index)
  └── Citizen Reports (Geo-Coordinates, Timestamp, Category, Photo)
                          │
                          ▼
            Spring Boot Ingestion Adapters
                          │
                          ▼
             Data Cleansing & Validation
  (Range checks, null handling, duplicate removal, coordinate bounds)
                          │
                          ▼
              H3 Spatial Normalization
       (Latitude / Longitude ──► Uber H3 Cell Index)
                          │
                          ▼
        Spatio-Temporal Aggregation & Feature Fusion
  (Lags: t-1h, t-3h; Wind vectors; Distance to fires; Station proximity)
                          │
                          ▼
                Model-Ready Feature Layer
                (Stored in `grid_features`)
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
   Hotspot Risk Detection      PM2.5 1-6h Forecast
             │                         │
             └────────────┬────────────┘
                          ▼
             Incident Assessment & Evidence
  (If Risk == HIGH: synthesize signals, compile evidence timeline)
                          │
                          ▼
             Gemini Qualitative Reasoning
         (Generate human-readable "WHY" narrative)
                          │
                          ▼
              Official Authority Alert
       (Dispatched to Municipal Incident Queue)
```

---

## 2. Spatial Normalization via Uber H3

The platform maps continuous planetary coordinates $(lat, lon)$ into discrete discrete hexagonal grid cells using the Uber H3 spatial index:

1. **Resolution Selection**:
   - **Resolution 7**: Area $\approx 5.16\text{ km}^2$, edge $\approx 1.22\text{ km}$. Used for city-wide macro heatmaps.
   - **Resolution 8**: Area $\approx 0.74\text{ km}^2$, edge $\approx 461\text{ m}$. Standard resolution for neighborhood hotspot detection and model features.
   - **Resolution 9**: Area $\approx 0.11\text{ km}^2$, edge $\approx 174\text{ m}$. Used for citizen incident clusters and immediate fire plume proximity.
2. **Spatial Feature Aggregation**:
   - Observations located within the polygon boundary of a cell are aggregated into hourly rolling statistics (mean, min, max, trend).
   - Inverse Distance Weighting (IDW) or spatial decay is computed from the cell centroid to the nearest fixed CAAQMS station and active thermal fire anomalies.

---

## 3. Data Cleansing & Quality Control Rules

| Parameter | Valid Range | Anomaly / Missing Rule |
|---|---|---|
| Latitude | $[-90.0, +90.0]$ | Discard observation if out of bounds |
| Longitude | $[-180.0, +180.0]$ | Discard observation if out of bounds |
| PM2.5 ($\mu\text{g/m}^3$) | $[0.0, 1000.0]$ | Negative values rejected; $>1000$ flagged as sensor anomaly |
| PM10 ($\mu\text{g/m}^3$) | $[0.0, 2000.0]$ | Negative values rejected |
| Wind Speed ($\text{m/s}$) | $[0.0, 75.0]$ | Negative values rejected |
| Wind Direction ($^\circ$) | $[0.0, 360.0]$ | Modulo 360 applied |
| Citizen Photos | JPEG, PNG, WebP | Max 10MB; validated via MIME type and Magic Bytes |

---

## 4. Fallback & Synthetic Data Strategy

To ensure zero-downtime demonstration resilience:
1. **Primary**: Live REST queries to external endpoints.
2. **Secondary**: Local relational cache (last observed valid values).
3. **Tertiary (Offline / Demo)**: Synthetic data generator script (`generate_synthetic_data.py`) which seeds realistic diurnal pollution profiles, temperature inversions, and seasonal burning patterns into `data/sample/`.
