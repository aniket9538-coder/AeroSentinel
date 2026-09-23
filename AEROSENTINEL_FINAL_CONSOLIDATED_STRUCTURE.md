# AeroSentinel — FINAL CONSOLIDATED PROJECT STRUCTURE

## Purpose

This is the final consolidated structure created by analyzing BOTH project structures:

1. The detailed `climate-intelligence-platform` structure, which contains the broader production-style organization, role-based frontend pages, complete Spring Boot controller/service/repository/entity/DTO layers, multiple data integrations, ML submodules, GIS data, infrastructure, storage and CI/CD.
2. The earlier `AeroSentinel` structure, which locks the product direction around the 7-day prototype: Java Spring Boot as the main backend, React frontend, Python FastAPI as the AI/ML service, PostgreSQL/PostGIS, H3, Gemini, pollution-event lifecycle, authority workflow, monitoring coverage and federated prototype.

The final structure keeps the important capabilities from both, while separating the 7-day MVP from advanced/future modules so the repository is complete without making the team implement everything at once.

---

# 1. FINAL ARCHITECTURE

```text
                         ┌─────────────────────────┐
                         │       React Frontend    │
                         │  Public / Analyst /     │
                         │  Authority / Admin      │
                         └────────────┬────────────┘
                                      │ REST
                                      ▼
                         ┌─────────────────────────┐
                         │   Java 21 Spring Boot   │
                         │     MAIN BACKEND        │
                         │                         │
                         │ Auth / APIs / Business  │
                         │ Data / Events / Alerts  │
                         │ Authority / Monitoring  │
                         │ External Integrations   │
                         └───────┬─────────┬───────┘
                                 │         │
                         JDBC/JPA│         │REST
                                 │         ▼
                                 │  ┌───────────────────┐
                                 │  │ Python FastAPI    │
                                 │  │ AI / ML Service   │
                                 │  │                   │
                                 │  │ Hotspot           │
                                 │  │ Forecast          │
                                 │  │ Evidence          │
                                 │  │ Gemini             │
                                 │  │ Vision             │
                                 │  └─────────┬─────────┘
                                 │            │
                                 ▼            ▼
                         ┌────────────┐  ┌─────────────┐
                         │ PostgreSQL │  │   Gemini    │
                         │ + PostGIS  │  │    API      │
                         └────────────┘  └─────────────┘

External data:
CPCB/OpenAQ ─┐
IMD/Weather ─┤
NASA FIRMS ──┤──> Spring Boot / Data Pipeline
Sentinel-5P ─┤
Citizen ─────┘

Federated:
Pune ─┐
Mumbai├──> Coordinator/Aggregator ──> Global Model
Delhi ─┘
```

## Architecture rules

- Spring Boot is the main backend and system orchestrator.
- Python is ONLY the AI/ML service.
- Frontend never connects directly to PostgreSQL.
- Frontend never exposes Gemini/API secrets.
- External data should be normalized before entering the core data model.
- H3 is the common spatial representation for hotspot/risk aggregation.
- Gemini explains/analyses validated evidence; it is not the numerical pollution predictor.
- Citizen reports are evidence and do not directly create a high-risk official alert.
- Federated learning is a prototype/federated-ready demonstration in the 7-day build.

---

# 2. COMPLETE FINAL REPOSITORY

```text
aerosentinel/
│
├── README.md
├── LICENSE
├── .gitignore
├── .env.example
├── docker-compose.yml
├── Makefile
│
├── docs/
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── DATA_FLOW.md
│   ├── API_CONTRACT.md
│   ├── DATABASE_SCHEMA.md
│   ├── ML_ARCHITECTURE.md
│   ├── DATASETS.md
│   ├── FEDERATED_ARCHITECTURE.md
│   ├── SECURITY.md
│   ├── DEPLOYMENT.md
│   ├── DEMO_SCRIPT.md
│   └── FUTURE_SCOPE.md
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   │
│   ├── public/
│   │   ├── icons/
│   │   └── assets/
│   │
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       │
│       ├── assets/
│       │
│       ├── components/
│       │   ├── common/
│       │   │   ├── Button.tsx
│       │   │   ├── Card.tsx
│       │   │   ├── Modal.tsx
│       │   │   ├── Badge.tsx
│       │   │   ├── Loading.tsx
│       │   │   └── ErrorState.tsx
│       │   │
│       │   ├── layout/
│       │   │   ├── Navbar.tsx
│       │   │   ├── Sidebar.tsx
│       │   │   └── PageContainer.tsx
│       │   │
│       │   ├── map/
│       │   │   ├── PollutionMap.tsx
│       │   │   ├── H3RiskLayer.tsx
│       │   │   ├── SensorLayer.tsx
│       │   │   ├── FireLayer.tsx
│       │   │   ├── SatelliteLayer.tsx
│       │   │   ├── CitizenReportLayer.tsx
│       │   │   └── MonitoringCoverageLayer.tsx
│       │   │
│       │   ├── charts/
│       │   │   ├── PM25Chart.tsx
│       │   │   ├── AQIChart.tsx
│       │   │   ├── PollutantTrendChart.tsx
│       │   │   ├── ForecastChart.tsx
│       │   │   ├── WindChart.tsx
│       │   │   └── ConfidenceChart.tsx
│       │   │
│       │   ├── hotspot/
│       │   │   ├── HotspotCard.tsx
│       │   │   ├── HotspotDetails.tsx
│       │   │   └── EvidencePanel.tsx
│       │   │
│       │   ├── events/
│       │   │   ├── EventCard.tsx
│       │   │   ├── EventTimeline.tsx
│       │   │   └── EventDetails.tsx
│       │   │
│       │   ├── forecasting/
│       │   │   ├── ForecastSummary.tsx
│       │   │   ├── ForecastTimeline.tsx
│       │   │   └── ForecastConfidence.tsx
│       │   │
│       │   ├── citizen/
│       │   │   ├── ReportForm.tsx
│       │   │   ├── ImageUploader.tsx
│       │   │   └── ReportStatus.tsx
│       │   │
│       │   ├── alerts/
│       │   │   ├── AlertCard.tsx
│       │   │   ├── AlertList.tsx
│       │   │   └── AlertDetails.tsx
│       │   │
│       │   ├── authority/
│       │   │   ├── IncidentQueue.tsx
│       │   │   ├── ActionPanel.tsx
│       │   │   ├── InspectionForm.tsx
│       │   │   └── ResolutionPanel.tsx
│       │   │
│       │   ├── evidence/
│       │   │   ├── EvidenceTimeline.tsx
│       │   │   ├── EvidenceItem.tsx
│       │   │   └── GeminiExplanation.tsx
│       │   │
│       │   └── federated/
│       │       ├── CityNodeCard.tsx
│       │       ├── FederatedStatus.tsx
│       │       └── ModelVersionCard.tsx
│       │
│       ├── pages/
│       │   ├── public/
│       │   │   ├── Home.tsx
│       │   │   ├── Dashboard.tsx
│       │   │   ├── PollutionMap.tsx
│       │   │   ├── Forecast.tsx
│       │   │   ├── Hotspots.tsx
│       │   │   └── CitizenReport.tsx
│       │   │
│       │   ├── analyst/
│       │   │   ├── AnalystDashboard.tsx
│       │   │   ├── HotspotAnalysis.tsx
│       │   │   ├── EvidenceAnalysis.tsx
│       │   │   └── ForecastAnalysis.tsx
│       │   │
│       │   ├── authority/
│       │   │   ├── AuthorityDashboard.tsx
│       │   │   ├── Alerts.tsx
│       │   │   ├── IncidentQueue.tsx
│       │   │   ├── Inspection.tsx
│       │   │   └── Actions.tsx
│       │   │
│       │   ├── admin/
│       │   │   ├── AdminDashboard.tsx
│       │   │   ├── Users.tsx
│       │   │   ├── Cities.tsx
│       │   │   └── SystemStatus.tsx
│       │   │
│       │   └── federated/
│       │       └── FederatedNetwork.tsx
│       │
│       ├── services/
│       │   ├── api.ts
│       │   ├── auth.service.ts
│       │   ├── air.service.ts
│       │   ├── weather.service.ts
│       │   ├── fire.service.ts
│       │   ├── satellite.service.ts
│       │   ├── hotspot.service.ts
│       │   ├── forecast.service.ts
│       │   ├── event.service.ts
│       │   ├── evidence.service.ts
│       │   ├── citizen.service.ts
│       │   ├── alert.service.ts
│       │   ├── authority.service.ts
│       │   ├── monitoring.service.ts
│       │   └── federated.service.ts
│       │
│       ├── hooks/
│       ├── store/
│       ├── types/
│       ├── utils/
│       └── styles/
│
├── backend/
│   ├── pom.xml
│   │
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── aerosentinel/
│       │   │           ├── AeroSentinelApplication.java
│       │   │
│       │   │           ├── config/
│       │   │           │   ├── SecurityConfig.java
│       │   │           │   ├── DatabaseConfig.java
│       │   │           │   ├── CorsConfig.java
│       │   │           │   ├── WebConfig.java
│       │   │           │   └── AiServiceConfig.java
│       │   │
│       │   │           ├── auth/
│       │   │           │   ├── AuthController.java
│       │   │           ├── AuthService.java
│       │   │           └── SecurityService.java
│       │   │
│       │   │           ├── city/
│       │   │           │   ├── City.java
│       │   │           │   ├── CityRepository.java
│       │   │           │   ├── CityService.java
│       │   │           │   └── CityController.java
│       │   │
│       │   │           ├── air/
│       │   │           │   ├── AirObservation.java
│       │   │           │   ├── AirObservationRepository.java
│       │   │           │   ├── AirService.java
│       │   │           │   └── AirController.java
│       │   │
│       │   │           ├── weather/
│       │   │           │   ├── WeatherObservation.java
│       │   │           │   ├── WeatherRepository.java
│       │   │           │   ├── WeatherService.java
│       │   │           │   └── WeatherController.java
│       │   │
│       │   │           ├── fire/
│       │   │           │   ├── FireEvent.java
│       │   │           │   ├── FireRepository.java
│       │   │           │   ├── FireService.java
│       │   │           │   └── FireController.java
│       │   │
│       │   │           ├── satellite/
│       │   │           │   ├── SatelliteObservation.java
│       │   │           │   ├── SatelliteRepository.java
│       │   │           │   ├── SatelliteService.java
│       │   │           │   └── SatelliteController.java
│       │   │
│       │   │           ├── sensor/
│       │   │           │   ├── MonitoringStation.java
│       │   │           │   ├── SensorRepository.java
│       │   │           │   ├── SensorService.java
│       │   │           │   └── SensorController.java
│       │   │
│       │   │           ├── grid/
│       │   │           │   ├── GridCell.java
│       │   │           │   ├── GridFeature.java
│       │   │           │   ├── GridRepository.java
│       │   │           │   ├── GridService.java
│       │   │           │   └── GridController.java
│       │   │
│       │   │           ├── citizen/
│       │   │           │   ├── CitizenReport.java
│       │   │           │   ├── CitizenReportRepository.java
│       │   │           │   ├── CitizenReportService.java
│       │   │           │   └── CitizenReportController.java
│       │   │
│       │   │           ├── hotspot/
│       │   │           │   ├── HotspotPrediction.java
│       │   │           │   ├── HotspotRepository.java
│       │   │           │   ├── HotspotService.java
│       │   │           │   └── HotspotController.java
│       │   │
│       │   │           ├── forecast/
│       │   │           │   ├── Forecast.java
│       │   │           │   ├── ForecastRepository.java
│       │   │           │   ├── ForecastService.java
│       │   │           │   └── ForecastController.java
│       │   │
│       │   │           ├── evidence/
│       │   │           │   ├── EventEvidence.java
│       │   │           │   ├── EvidenceRepository.java
│       │   │           │   ├── EvidenceService.java
│       │   │           │   └── EvidenceController.java
│       │   │
│       │   │           ├── event/
│       │   │           │   ├── PollutionEvent.java
│       │   │           │   ├── PollutionEventRepository.java
│       │   │           │   ├── PollutionEventService.java
│       │   │           │   └── PollutionEventController.java
│       │   │
│       │   │           ├── alert/
│       │   │           │   ├── Alert.java
│       │   │           │   ├── AlertRepository.java
│       │   │           │   ├── AlertService.java
│       │   │           │   └── AlertController.java
│       │   │
│       │   │           ├── authority/
│       │   │           │   ├── Authority.java
│       │   │           │   ├── AuthorityRepository.java
│       │   │           │   ├── AuthorityService.java
│       │   │           │   └── AuthorityController.java
│       │   │
│       │   │           ├── action/
│       │   │           │   ├── AuthorityAction.java
│       │   │           │   ├── ActionRepository.java
│       │   │           │   ├── ActionService.java
│       │   │           │   └── ActionController.java
│       │   │
│       │   │           ├── inspection/
│       │   │           │   ├── Inspection.java
│       │   │           │   ├── InspectionRepository.java
│       │   │           │   ├── InspectionService.java
│       │   │           │   └── InspectionController.java
│       │   │
│       │   │           ├── monitoring/
│       │   │           │   ├── MonitoringPriority.java
│       │   │           │   ├── MonitoringService.java
│       │   │           │   └── MonitoringController.java
│       │   │
│       │   │           ├── federated/
│       │   │           │   ├── FederatedNode.java
│       │   │           │   ├── ModelUpdate.java
│       │   │           │   ├── FederatedService.java
│       │   │           │   └── FederatedController.java
│       │   │
│       │   │           ├── dto/
│       │   │           │   ├── auth/
│       │   │           │   ├── air/
│       │   │           │   ├── weather/
│       │   │           │   ├── fire/
│       │   │           │   ├── satellite/
│       │   │           │   ├── citizen/
│       │   │           │   ├── grid/
│       │   │           │   ├── hotspot/
│       │   │           │   ├── forecast/
│       │   │           │   ├── event/
│       │   │           │   ├── evidence/
│       │   │           │   ├── alert/
│       │   │           │   ├── authority/
│       │   │           │   ├── action/
│       │   │           │   ├── inspection/
│       │   │           │   ├── monitoring/
│       │   │           │   └── federated/
│       │   │
│       │   │           ├── integration/
│       │   │           │   ├── ai/
│       │   │           │   │   ├── AiServiceClient.java
│       │   │           │   │   ├── HotspotClient.java
│       │   │           │   │   ├── ForecastClient.java
│       │   │           │   │   ├── EvidenceClient.java
│       │   │           │   │   └── VisionClient.java
│       │   │           │   │
│       │   │           │   ├── cpcb/
│       │   │           │   │   └── CpcbClient.java
│       │   │           │   ├── weather/
│       │   │           │   │   └── WeatherClient.java
│       │   │           │   ├── firms/
│       │   │           │   │   └── FirmsClient.java
│       │   │           │   └── satellite/
│       │   │           │       └── SatelliteClient.java
│       │   │
│       │   │           ├── security/
│       │   │           │   ├── JwtAuthenticationFilter.java
│       │   │           │   ├── JwtService.java
│       │   │           │   ├── CustomUserDetailsService.java
│       │   │           │   └── Role.java
│       │   │
│       │   │           ├── exception/
│       │   │           │   ├── GlobalExceptionHandler.java
│       │   │           │   ├── ResourceNotFoundException.java
│       │   │           │   └── ValidationException.java
│       │   │
│       │   │           └── util/
│       │   │               ├── GeoUtils.java
│       │   │               ├── H3Utils.java
│       │   │               ├── DateTimeUtils.java
│       │   │               └── ValidationUtils.java
│       │   │
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/
│               └── migration/
│                   ├── V1__init_schema.sql
│                   ├── V2__seed_reference_data.sql
│                   └── V3__spatial_indexes.sql
│
├── ai-service/
│   ├── requirements.txt
│   ├── pyproject.toml
│   ├── README.md
│   ├── Dockerfile
│   │
│   ├── app/
│   │   ├── main.py
│   │   ├── api/
│   │   │   ├── hotspot.py
│   │   │   ├── forecast.py
│   │   │   ├── evidence.py
│   │   │   ├── gemini.py
│   │   │   └── vision.py
│   │   │
│   │   ├── schemas/
│   │   │   ├── hotspot.py
│   │   │   ├── forecast.py
│   │   │   ├── evidence.py
│   │   │   └── vision.py
│   │   │
│   │   ├── models/
│   │   │   ├── hotspot_model.py
│   │   │   └── forecast_model.py
│   │   │
│   │   ├── features/
│   │   │   ├── air_features.py
│   │   │   ├── weather_features.py
│   │   │   ├── fire_features.py
│   │   │   ├── satellite_features.py
│   │   │   ├── spatial_features.py
│   │   │   └── temporal_features.py
│   │   │
│   │   ├── services/
│   │   │   ├── hotspot_service.py
│   │   │   ├── forecast_service.py
│   │   │   ├── evidence_service.py
│   │   │   ├── gemini_service.py
│   │   │   └── vision_service.py
│   │   │
│   │   └── utils/
│   │
│   ├── ingestion/
│   │   ├── air_quality/
│   │   ├── weather/
│   │   ├── fires/
│   │   └── satellite/
│   │
│   ├── preprocessing/
│   │   ├── cleaning.py
│   │   ├── normalization.py
│   │   ├── temporal_alignment.py
│   │   ├── spatial_alignment.py
│   │   └── missing_values.py
│   │
│   ├── training/
│   │   ├── train_hotspot.py
│   │   └── train_forecast.py
│   │
│   ├── evaluation/
│   │   ├── hotspot_metrics.py
│   │   ├── forecast_metrics.py
│   │   ├── classification_metrics.py
│   │   ├── alert_metrics.py
│   │   └── lead_time.py
│   │
│   ├── explainability/
│   │   ├── prediction_explanation.py
│   │   └── shap_analysis.py
│   │
│   ├── notebooks/
│   │   ├── 01_data_exploration.ipynb
│   │   ├── 02_air_quality.ipynb
│   │   ├── 03_weather.ipynb
│   │   ├── 04_satellite.ipynb
│   │   ├── 05_fire_detection.ipynb
│   │   ├── 06_hotspot_detection.ipynb
│   │   ├── 07_forecasting.ipynb
│   │   └── 08_model_evaluation.ipynb
│   │
│   └── artifacts/
│       ├── models/
│       ├── scalers/
│       ├── encoders/
│       └── explainers/
│
├── data/
│   ├── raw/
│   │   ├── air_quality/
│   │   ├── weather/
│   │   ├── fires/
│   │   ├── satellite/
│   │   └── citizen/
│   │
│   ├── interim/
│   ├── processed/
│   ├── features/
│   ├── synthetic/
│   │
│   ├── sample/
│   │   ├── air_quality.csv
│   │   ├── weather.csv
│   │   ├── fires.csv
│   │   └── citizen_reports.json
│   │
│   └── geo/
│       ├── cities.geojson
│       ├── roads.geojson
│       ├── industrial_zones.geojson
│       ├── agricultural_zones.geojson
│       ├── schools.geojson
│       ├── hospitals.geojson
│       └── monitoring_stations.geojson
│
├── federated/
│   ├── coordinator/
│   │   ├── coordinator.py
│   │   ├── aggregator.py
│   │   ├── round_manager.py
│   │   └── model_registry.py
│   │
│   ├── clients/
│   │   ├── pune/
│   │   │   ├── client.py
│   │   │   ├── trainer.py
│   │   │   └── local_data.py
│   │   ├── mumbai/
│   │   │   ├── client.py
│   │   │   ├── trainer.py
│   │   │   └── local_data.py
│   │   └── delhi/
│   │       ├── client.py
│   │       ├── trainer.py
│   │       └── local_data.py
│   │
│   ├── models/
│   │   ├── local_model.py
│   │   └── global_model.py
│   │
│   └── privacy/
│       ├── differential_privacy.py
│       └── secure_aggregation.py
│
├── database/
│   ├── init.sql
│   ├── schema.sql
│   └── seed.sql
│
├── scripts/
│   ├── setup.sh
│   ├── setup_db.sh
│   ├── seed_database.py
│   ├── seed_data.py
│   ├── generate_synthetic_data.py
│   ├── download_firms.py
│   ├── download_weather.py
│   ├── prepare_features.py
│   ├── run_pipeline.py
│   ├── train_models.py
│   ├── evaluate_models.py
│   └── create_demo_event.py
│
├── infrastructure/
│   ├── docker/
│   │   ├── frontend.Dockerfile
│   │   ├── backend.Dockerfile
│   │   ├── ai.Dockerfile
│   │   └── federated.Dockerfile
│   │
│   ├── nginx/
│   │   └── nginx.conf
│   │
│   └── monitoring/
│       ├── prometheus.yml
│       └── grafana/
│
├── storage/
│   ├── uploads/
│   │   └── citizen_reports/
│   ├── satellite/
│   ├── model_artifacts/
│   └── logs/
│
├── tests/
│   ├── frontend/
│   ├── backend/
│   ├── ai/
│   ├── federated/
│   └── e2e/
│
└── .github/
    └── workflows/
        ├── frontend.yml
        ├── backend.yml
        ├── ai.yml
        └── docker.yml
```

---

# 3. FRONTEND RESPONSIBILITY

The frontend supports four logical user experiences:

```text
PUBLIC
  ↓
see map / pollution / forecast / report issue

ANALYST
  ↓
inspect hotspot / evidence / forecast

AUTHORITY
  ↓
receive alert / assign / inspect / act / resolve

ADMIN
  ↓
manage users / cities / system status
```

The detailed friend structure already included public, analyst, authority and admin page separation, plus map, charts, events, citizen, authority, forecasting, evidence and federated components.

For the 7-day demo:
- Public
- Analyst
- Authority
- Federated

are the main visible flows.

Admin remains a lightweight management layer unless time permits.

---

# 4. BACKEND RESPONSIBILITY

Spring Boot is responsible for:

```text
Authentication
      ↓
API Gateway / REST APIs
      ↓
Data persistence
      ↓
External data integrations
      ↓
AI-service orchestration
      ↓
Pollution event lifecycle
      ↓
Alerts
      ↓
Authority workflow
      ↓
Monitoring priority
      ↓
Federated status
```

The detailed controller/service/repository/entity/DTO organization from the friend structure is retained conceptually, but domain modules are grouped more cleanly in the final structure.

---

# 5. AI / ML RESPONSIBILITY

The AI service handles:

```text
Raw/normalized features
       ↓
Feature engineering
       ↓
Hotspot detection
       ↓
Short-term forecast
       ↓
Confidence
       ↓
Evidence fusion
       ↓
Gemini explanation
```

## Main models

### Hotspot
Use:
- anomaly detection
- spatial/cluster logic
- risk scoring
- XGBoost where supervised labels are available

### Forecast
Use:
- baseline
- XGBoost/LightGBM style model

Do NOT make LSTM/Transformer a Day-1 dependency.

### Computer Vision
For the MVP:
- Gemini Vision is sufficient for citizen-submitted image interpretation.

A larger custom CV pipeline can remain future scope.

### Source Attribution / Plume
Keep these as documented extension points, not core 7-day dependencies.

Do not present correlation as proven causal source attribution.

---

# 6. DATA ARCHITECTURE

```text
Air Quality
Weather
Fire
Satellite
Citizen
Monitoring Stations
       ↓
Normalization
       ↓
Temporal Alignment
       ↓
Spatial Alignment
       ↓
H3 Mapping
       ↓
Grid Features
       ↓
AI Models
```

## Core data folders

```text
raw
 ↓
interim
 ↓
processed
 ↓
features
```

Synthetic and sample data are retained so the demo does not fail if an external API is unavailable.

---

# 7. CORE DATABASE MODEL

The final core tables are:

```text
users
cities
monitoring_stations

air_observations
weather_observations
fire_events
satellite_observations

grid_cells
grid_features

hotspot_predictions
forecasts

citizen_reports
gemini_analyses

pollution_events
event_evidence

alerts
authority_actions
inspections

federated_nodes
model_updates
```

Additional reference/geospatial layers:

```text
roads
industrial_zones
agricultural_zones
schools
hospitals
```

These are useful for spatial context and future exposure/source analysis.

---

# 8. POLLUTION EVENT LIFECYCLE

This is the central operational workflow:

```text
Air + Weather + Fire + Satellite + Citizen
                    ↓
              H3 Grid Cell
                    ↓
             Risk Detection
                    ↓
            Potential Event
                    ↓
              Evidence
                    ↓
             Short Forecast
                    ↓
           Gemini Explanation
                    ↓
                  Alert
                    ↓
            Authority Queue
                    ↓
             Team Assigned
                    ↓
             Field Inspection
                    ↓
              Action Taken
                    ↓
                Resolution
```

Example:

```text
EVENT-1023

Location: H3-XYZ
Started: 10:30 AM
Risk: HIGH

Evidence:
- PM2.5 rising
- Low wind
- Fire nearby
- Citizen report

Forecast:
- spike expected in approximately 2 hours

Status:
OPEN
```

Then:

```text
OPEN
 ↓
ASSIGNED
 ↓
FIELD VERIFICATION
 ↓
ACTION TAKEN
 ↓
RESOLVED
```

---

# 9. MONITORING COVERAGE

For each H3 cell:

```text
station_distance =
distance(H3 cell, nearest monitoring station)
```

Combine:

```text
Risk
+
Uncertainty
+
Station Distance
```

Example decision:

```text
High risk
High uncertainty
Far from station
        ↓
Monitoring Priority HIGH
        ↓
Recommend mobile sensor / field verification
```

This is a key AeroSentinel differentiator.

---

# 10. CITIZEN WORKFLOW

```text
Citizen
  ↓
Location
  ↓
Photo + Description
  ↓
Spring Boot
  ↓
AI Service
  ↓
Gemini Vision
  ↓
Structured Observation
  ↓
H3 Mapping
  ↓
Evidence
  ↓
Potential Event
```

Citizen reports should be treated as one evidence source.

They should not automatically create an official high-risk alert.

---

# 11. FEDERATED ARCHITECTURE

```text
                 Coordinator
                     │
        ┌────────────┼────────────┐
        ↓            ↓            ↓
      Pune         Mumbai       Delhi
       Node          Node         Node
        │            │            │
    Local Model  Local Model  Local Model
        │            │            │
        └────────────┼────────────┘
                     ↓
               Model Updates
                     ↓
                Aggregation
                     ↓
                Global Model
```

The friend's structure also included:

```text
privacy/
├── differential_privacy.py
└── secure_aggregation.py
```

These are retained in the repository as future/advanced modules, but they are NOT required for the 7-day MVP.

For the hackathon demo, show:
- Pune node
- Mumbai node
- Delhi node
- local model/update
- coordinator
- aggregation
- global model version

Clearly label this as a prototype/federated-ready demonstration.

---

# 12. EXTERNAL DATA INTEGRATIONS

Main sources represented in the two structures:

```text
CPCB / OpenAQ
      ↓
Air quality

IMD / Weather source
      ↓
Weather

NASA FIRMS
      ↓
Fire detections

Sentinel-5P / Earth Engine
      ↓
Satellite atmospheric indicators

Citizen reports
      ↓
Crowdsourced evidence
```

Additional GIS reference data:

```text
cities.geojson
roads.geojson
industrial_zones.geojson
agricultural_zones.geojson
schools.geojson
hospitals.geojson
monitoring_stations.geojson
```

If a live source is unavailable during the demo, use clearly labelled sample/synthetic/cached data.

---

# 13. IMPORTANT SAFETY / SCIENTIFIC FRAMING

The system must NOT claim:

- It replaces official AQI.
- A fire detection proves pollution causality.
- Satellite NO2/SO2 is ground-level PM2.5.
- A correlated location is definitively the pollution source.
- Gemini is the numerical pollution prediction engine.

Use:

```text
Potential Hotspot
Contributing Signals
Evidence
Model Risk Score
Forecast
Confidence
Field Verification Recommended
```

Always show:

```text
timestamp
data source
live/historical/simulated status
confidence/uncertainty
```

---

# 14. 7-DAY MVP PRIORITY

## P0 — MUST WORK

```text
Air
Weather
H3
Hotspot
Forecast
Dashboard
Gemini
Citizen Report
Alert
Authority Workflow
Deployment
```

## P1 — STRONG DIFFERENTIATORS

```text
NASA FIRMS
Satellite
Monitoring Coverage
Evidence Chain
Sensor Recommendation
Federated Network
Common Data Schema
```

## P2 — FUTURE / ONLY IF TIME REMAINS

```text
Advanced plume visualization
Advanced source attribution
LSTM
Transformer
Large custom CV pipeline
Advanced exposure modelling
Voice
Multilingual
What-if simulation
Production secure aggregation
Differential privacy
Redis
Prometheus/Grafana
Kubernetes
Large admin system
```

---

# 15. 3-MEMBER OWNERSHIP

## MEMBER 1 — BACKEND + ARCHITECTURE + INTEGRATION

Own:

```text
Spring Boot
PostgreSQL/PostGIS
Flyway
REST APIs
Authentication
Business logic
Air/Weather/Fire/Satellite persistence
External integrations
AI-service integration
Pollution Events
Evidence
Alerts
Authority workflow
Inspection
Actions
Monitoring
Federated API
Deployment coordination
```

Main backend domains:

```text
City
Air
Weather
Fire
Satellite
Sensor
Grid
Citizen
Hotspot
Forecast
Evidence
Event
Alert
Authority
Action
Inspection
Monitoring
Federated
Integration
```

---

## MEMBER 2 — FRONTEND + UX

Own:

```text
React
TypeScript
Vite
Map
Dashboard
Charts
Hotspots
Evidence
Citizen UI
Alerts
Authority UI
Inspection UI
Federated UI
Analyst UI
Basic Admin UI
```

Main pages:

```text
Public
Analyst
Authority
Admin
Federated
```

---

## MEMBER 3 — AI/ML + DATA + GIS

Own:

```text
Python FastAPI
Data cleaning
Data preprocessing
Feature engineering
H3/GIS
Hotspot model
Forecast model
Evidence fusion
Confidence
Gemini
Gemini Vision
Model evaluation
Data ingestion scripts
Federated local models
Federated aggregation
```

To balance workload:

```text
Member 3:
Model + Features + Gemini

Member 1:
External API plumbing + persistence + AI orchestration

Member 2:
Frontend integration
```

---

# 16. DEVELOPMENT PHASES

## PHASE 0 — ARCHITECTURE LOCK

All members:

```text
PRD
Architecture
Database
API Contract
Git repository
Branches
Environment variables
Docker compose
```

## PHASE 1 — FOUNDATION

Member 1:
```text
Spring Boot
PostGIS
Flyway
Security
Base APIs
```

Member 2:
```text
React
Routing
Layout
Map
Dashboard shell
```

Member 3:
```text
Python
Dataset inventory
Cleaning
Baseline model
```

## PHASE 2 — DATA LAYER

```text
Air
Weather
Fire
Satellite
Common schema
H3
Grid features
```

## PHASE 3 — INTELLIGENCE

```text
Hotspot
Forecast
Confidence
Evidence
```

## PHASE 4 — GOOGLE AI

```text
Citizen Photo
      ↓
Gemini Vision

Prediction + Evidence
      ↓
Gemini Explanation
```

## PHASE 5 — ACTION

```text
Prediction
 ↓
Pollution Event
 ↓
Alert
 ↓
Authority Queue
 ↓
Assignment
 ↓
Inspection
 ↓
Action
 ↓
Resolution
```

## PHASE 6 — MONITORING

```text
Risk
+
Uncertainty
+
Station Distance
 ↓
Monitoring Priority
```

## PHASE 7 — FEDERATED

```text
Pune
Mumbai
Delhi
 ↓
Local Updates
 ↓
Coordinator
 ↓
Aggregation
 ↓
Global Model
```

## PHASE 8 — FINAL INTEGRATION

```text
Frontend
 ↓
Backend
 ↓
Database
 ↓
AI
 ↓
Gemini
 ↓
Event
 ↓
Alert
 ↓
Authority
 ↓
Resolution
```

---

# 17. 7-DAY EXECUTION

## DAY 1
Architecture, repository, database, Spring Boot, React, Python setup.

## DAY 2
Air/weather ingestion, DB persistence, dashboard, map.

## DAY 3
H3, fire, satellite signals, grid features.

## DAY 4
Hotspot model, forecast model, evidence engine.

## DAY 5
Citizen reporting, Gemini Vision, Gemini explanation, alerts.

## DAY 6
Authority workflow, monitoring recommendation, federated prototype.

## DAY 7
Integration, tests, deployment, fallback data, demo rehearsal.

---

# 18. EVALUATION

Track:

```text
Forecast MAE
Forecast RMSE
Hotspot precision/recall where labels permit
Forecast lead time
Data freshness
API latency
AI inference latency
Alert generation time
End-to-end workflow success
Gemini success rate
Multi-city federated demonstration
```

---

# 19. TEST STRUCTURE

```text
tests/
├── frontend/
├── backend/
├── ai/
├── federated/
└── e2e/
```

Minimum critical E2E test:

```text
Data
 ↓
H3
 ↓
Hotspot
 ↓
Forecast
 ↓
Evidence
 ↓
Event
 ↓
Alert
 ↓
Authority
 ↓
Inspection
 ↓
Action
 ↓
Resolution
```

---

# 20. INFRASTRUCTURE

Repository keeps the infrastructure concepts from the broader structure:

```text
infrastructure/
├── docker/
│   ├── frontend.Dockerfile
│   ├── backend.Dockerfile
│   ├── ai.Dockerfile
│   └── federated.Dockerfile
│
├── nginx/
│   └── nginx.conf
│
└── monitoring/
    ├── prometheus.yml
    └── grafana/
```

For the 7-day build:
- Docker is useful.
- Nginx is optional.
- Monitoring stack is optional.
- Kubernetes is not required.

---

# 21. STORAGE

```text
storage/
├── uploads/
│   └── citizen_reports/
├── satellite/
├── model_artifacts/
└── logs/
```

Do not commit:
- API keys
- Gemini keys
- database passwords
- private credentials
- large generated datasets
- large model binaries unless intentionally versioned

---

# 22. CI/CD

```text
.github/
└── workflows/
    ├── frontend.yml
    ├── backend.yml
    ├── ai.yml
    └── docker.yml
```

CI/CD can initially run:
- build
- unit tests
- lint/checks

Deployment can be added after the core workflow works.

---

# 23. WHY THIS IS THE FINAL CONSOLIDATED VERSION

The friend's structure contributes important completeness:

```text
public assets
role-based pages
auth
sensor module
events
forecasting
evidence
full Spring layers
DTO separation
external integrations
GIS files
database scripts
infrastructure
storage
CI/CD
advanced ML extension points
```

The AeroSentinel structure contributes the locked product architecture:

```text
Java Spring Boot main backend
Python FastAPI AI service
PostGIS
H3
pollution event lifecycle
event evidence
Gemini reasoning
monitoring coverage
monitoring priority
citizen → evidence
authority → inspection → action → resolution
federated prototype
7-day prioritization
```

Therefore the final repository is intentionally:

```text
COMPLETE ARCHITECTURE
        +
WORKABLE 7-DAY MVP
        +
FUTURE EXTENSION POINTS
```

The team should NOT attempt to implement every directory in the 7-day MVP.

The directory is the complete product architecture.

The P0/P1/P2 list determines what is actually implemented first.
