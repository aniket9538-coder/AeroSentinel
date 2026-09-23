# AeroSentinel

> **AI-powered hyperlocal air pollution hotspot detection, short-term forecasting, evidence analysis and climate-action coordination platform.**

---

## 1. Executive Summary

Indian cities face severe air pollution challenges, yet traditional monitoring relies on sparse networks of fixed Continuous Ambient Air Quality Monitoring Stations (CAAQMS). These fixed stations measure ambient pollution accurately at their exact locations, but cannot observe neighborhood-level variations, seasonal burning, industrial smoke spikes, or localized traffic emissions occurring between stations.

**AeroSentinel** bridges this spatial and temporal monitoring gap. By fusing ground air-quality sensor data, meteorological feeds, satellite atmospheric indicators (Sentinel-5P / Google Earth Engine), NASA FIRMS thermal anomaly/fire detections, and citizen-submitted visual reports, AeroSentinel:
1. Maps multi-source environmental signals into discrete **Uber H3 hexagonal spatial cells**.
2. Runs Machine Learning models to identify **potential pollution hotspots** and compute short-term (1–6 hour) PM2.5 forecasts.
3. Synthesizes multi-source evidence and leverages **Google Gemini** for multimodal citizen image verification and clear, human-readable causal reasoning.
4. Generates structured, prioritized alerts for municipal environmental authorities through a full-lifecycle incident response workflow.
5. Demonstrates a **multi-city federated learning prototype** (Pune, Mumbai, Delhi) where local models train on city-specific patterns and share privacy-preserving updates with a global aggregator.

---

## 2. Core Architecture

```text
                         ┌─────────────────────────┐
                         │       React Frontend    │
                         │  Public / Analyst /     │
                         │  Authority / Admin      │
                         └────────────┬────────────┘
                                      │ REST API
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
                         JDBC/JPA│         │REST (JSON)
                                 │         ▼
                                 │  ┌───────────────────┐
                                 │  │ Python FastAPI    │
                                 │  │ AI / ML Service   │
                                 │  │                   │
                                 │  │ Hotspot Detection │
                                 │  │ PM2.5 Forecast    │
                                 │  │ Evidence Fusion   │
                                 │  │ Gemini Reasoning  │
                                 │  │ Gemini Vision     │
                                 │  └─────────┬─────────┘
                                 │            │
                                 ▼            ▼
                         ┌────────────┐  ┌─────────────┐
                         │ PostgreSQL │  │   Google    │
                         │ + PostGIS  │  │ Gemini API  │
                         └────────────┘  └─────────────┘

External Data Integrations:
  - CPCB / OpenAQ Ground Stations
  - IMD / OpenWeather Feeds
  - NASA FIRMS Active Fire Detections
  - Sentinel-5P Satellite Atmospheric Tropospheric Columns
  - Crowdsourced Citizen Observations & Imagery

Federated Learning Prototype:
  Pune Node ──┐
  Mumbai Node ├──> Federated Coordinator / Aggregator ──> Global Model
  Delhi Node ─┘
```

### Architectural Principles & Rules
1. **Spring Boot is the System Orchestrator**: The backend controls authentication, business workflows, spatial data persistence, alert dispatch, and authority task queues.
2. **Python is Strictly the AI/ML Service**: FastAPI serves internal inference endpoints; the frontend never bypasses Spring Boot.
3. **Database Isolation**: The frontend never connects directly to PostgreSQL/PostGIS.
4. **Zero Client Secrets**: Gemini and third-party API keys remain strictly server-side.
5. **H3 Common Spatial Grid**: All spatial features are indexed by Uber H3 cells for consistent cross-source aggregation.
6. **Gemini for Explanation & Multimodal Vision**: Gemini synthesizes evidence into human-readable narratives and analyzes citizen photos. Gemini is **not** used as a numerical pollution regressor.
7. **Citizen Reports as Evidence**: Citizen reports are treated as corroborating evidence and never directly trigger official critical alarms without verification.
8. **Graceful Fallback**: The platform provides synthetic/cached fallback data so demonstrations remain resilient even if third-party APIs are unreachable.

---

## 3. Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, TypeScript, Vite, React Router, Leaflet, React Leaflet, H3-js, Recharts, Lucide React |
| **Backend** | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security, Hibernate Spatial, PostGIS, Flyway, JWT |
| **Database** | PostgreSQL 16 + PostGIS 3.4 |
| **AI / ML Service** | Python 3.11+, FastAPI, Uvicorn, Pydantic, NumPy, pandas, scikit-learn, XGBoost, H3-py, Google GenAI SDK |
| **Spatial Layer** | Uber H3 Hexagonal Hierarchical Spatial Index (Resolution 7–9) |
| **Infrastructure** | Docker, Docker Compose, Nginx, GitHub Actions |

---

## 4. Key Workflows

### Pollution Event Lifecycle
```text
Raw Ingestion (Air, Weather, Fires, Satellite, Citizen)
        ↓
Spatial & Temporal Normalization (H3 Cell Indexing)
        ↓
Feature Vector Generation (Lags, Wind, Nearby Fires)
        ↓
ML Hotspot Risk Detection + 1–6h PM2.5 Forecast
        ↓
Potential Pollution Event Flagged
        ↓
Multi-Source Evidence Fusion + Gemini Explanation
        ↓
Authority Alert Generated & Ranked
        ↓
Authority Queue → Team Assignment → Field Inspection → Mitigation Action → Resolution
```

### Citizen Reporting Flow
```text
Citizen captures geo-tagged photo & report
        ↓
Spring Boot ingests multipart request & saves metadata
        ↓
FastAPI AI Service processes image with Gemini Vision
        ↓
Structured observation extracted (category: SMOKE/BURNING/DUST, confidence)
        ↓
Observation mapped to H3 grid cell as corroborating evidence
```

### Monitoring Coverage Optimization
For every H3 grid cell, AeroSentinel calculates a Monitoring Priority Index:
$$\text{Priority} = f(\text{Predicted Risk}, \text{Model Uncertainty}, \text{Distance to Nearest CAAQMS})$$
Cells with high risk, elevated uncertainty, and large distances from fixed sensors are automatically recommended for mobile sensor deployment or drone inspection.

---

## 5. 7-Day MVP Priorities

### P0 — Core MVP (Must Work)
- [x] End-to-end repository scaffolding, Docker orchestration, and CI/CD pipelines
- [x] Spring Boot REST API orchestrator with PostgreSQL + PostGIS schema
- [x] React + TypeScript dashboard with interactive Leaflet map and H3 grid visualization
- [x] Python FastAPI service with baseline hotspot risk scoring and PM2.5 forecasting
- [x] Google Gemini integration for evidence explanation and citizen photo verification
- [x] Incident queue and authority action workflow
- [x] Sample and synthetic datasets for air quality, weather, fire, and citizen reports

### P1 — Strong Differentiators
- [ ] Live CPCB/OpenAQ station ingestion pipeline
- [ ] NASA FIRMS real-time thermal anomaly ingestion
- [ ] Monitoring coverage priority index and sensor deployment recommendations
- [ ] 3-city federated prototype coordinator (Pune, Mumbai, Delhi)
- [ ] Multi-parameter pollutant trend charts (PM2.5, PM10, NO2, SO2, CO)

### P2 — Future Extensions
- Advanced plume dispersion modeling (CALPUFF/AERMOD)
- Deep learning sequence models (LSTM / Spatial-Temporal Graph Neural Networks)
- Differential privacy and secure aggregation for production federated learning
- Multilingual voice-enabled citizen reporting

---

## 6. Repository Structure

```text
aerosentinel/
├── README.md                           # Master project documentation
├── LICENSE                             # Apache 2.0 open-source license
├── .gitignore                          # Cross-stack git ignore configuration
├── .env.example                        # Template for environment secrets
├── docker-compose.yml                  # Full stack local orchestration
├── Makefile                            # Developer command shortcuts
│
├── docs/                               # Formal architectural documentation
│   ├── PRD.md                          # Product Requirements Document
│   ├── ARCHITECTURE.md                 # System architecture specification
│   ├── DATA_FLOW.md                    # Data pipeline and normalization flow
│   ├── API_CONTRACT.md                 # REST API endpoints & JSON contracts
│   ├── DATABASE_SCHEMA.md              # PostGIS entity relationship schema
│   ├── ML_ARCHITECTURE.md              # Hotspot, forecasting, and Gemini specs
│   ├── DATASETS.md                     # Data inventory & dictionary
│   ├── FEDERATED_ARCHITECTURE.md       # Multi-city federated learning design
│   ├── SECURITY.md                     # Authentication, CORS, secret protection
│   ├── DEPLOYMENT.md                   # Container & cloud deployment guide
│   ├── DEMO_SCRIPT.md                  # Hackathon live demonstration walkthrough
│   └── FUTURE_SCOPE.md                 # Phase 2 roadmap & research directions
│
├── frontend/                           # React + TypeScript + Vite web application
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── public/                         # Static assets & icons
│   └── src/
│       ├── components/                 # UI components organized by domain
│       ├── pages/                      # Public, Analyst, Authority, Admin pages
│       ├── services/                   # HTTP API client services
│       ├── hooks/                      # Custom React hooks
│       ├── store/                      # State management
│       ├── types/                      # TypeScript domain models
│       ├── utils/                      # Formatting & spatial utilities
│       └── styles/                     # CSS design tokens
│
├── backend/                            # Java 21 Spring Boot main orchestrator
│   ├── pom.xml                         # Maven dependencies & build configuration
│   └── src/
│       ├── main/java/com/aerosentinel/ # Controllers, Services, Repositories, Entities
│       └── main/resources/             # application.yml & Flyway migrations
│
├── ai-service/                         # Python FastAPI AI/ML microservice
│   ├── requirements.txt                # ML dependencies
│   ├── pyproject.toml                  # Python package configuration
│   ├── Dockerfile
│   └── app/                            # API endpoints, schemas, models, Gemini services
│
├── data/                               # Data storage & sample files
│   ├── raw/, interim/, processed/
│   ├── sample/                         # Seed CSVs for air, weather, fires, citizen reports
│   └── geo/                            # GeoJSON layers for cities, roads, infrastructure
│
├── federated/                          # Multi-city federated prototype
│   ├── coordinator/                    # Aggregator & round manager
│   ├── clients/                        # Pune, Mumbai, Delhi client nodes
│   ├── models/                         # Local and global model representations
│   └── privacy/                        # Differential privacy & aggregation scaffolds
│
├── database/                           # Database setup & reference SQL
│   ├── init.sql                        # PostGIS initialization
│   ├── schema.sql                      # Complete schema definition
│   └── seed.sql                        # Reference reference data
│
├── scripts/                            # Automation, ETL, and utility scripts
├── infrastructure/                     # Dockerfiles, Nginx, and monitoring configs
├── storage/                            # Local storage for uploads, logs, and artifacts
├── tests/                              # Unit, integration, and E2E test suites
└── .github/workflows/                  # GitHub Actions CI/CD pipelines
```

---

## 7. Local Setup & Quickstart

### Prerequisites
- **Java**: OpenJDK 21 LTS
- **Maven**: 3.9+ (or use the provided wrapper)
- **Node.js**: v20+ and npm 10+
- **Python**: 3.11+
- **Docker & Docker Compose** (Optional for full containerized stack)

### 1. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Provide your `GEMINI_API_KEY` in `.env` if testing live Google GenAI capabilities.

### 2. Run with Docker Compose (Recommended)
```bash
docker compose up --build
```
- Frontend: `http://localhost:3000`
- Spring Boot Backend: `http://localhost:8080/actuator/health`
- FastAPI AI Service: `http://localhost:8000/health`
- PostgreSQL: `localhost:5432`

### 3. Run Manually for Local Development

#### Start PostgreSQL + PostGIS:
Ensure PostgreSQL is running locally with PostGIS extension enabled on port 5432.

#### Start the AI Service:
```bash
cd ai-service
python -m venv .venv
# On Windows:
.venv\Scripts\activate
# On Linux/macOS:
# source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

#### Start Spring Boot Backend:
```bash
cd backend
./mvnw spring-boot:run
```

#### Start React Frontend:
```bash
cd frontend
npm install
npm run dev
```
Visit `http://localhost:5173` (or the Vite dev URL shown in terminal).

---

## 8. Verification & Testing

Execute the test suites across each tier:
```bash
# Frontend Build Verification
cd frontend && npm run build

# Backend Maven Tests
cd backend && ./mvnw test

# AI Service Smoke Tests
cd ai-service && pytest tests/
```

---

## 9. Contributors & License

Developed under the Apache 2.0 License. See [LICENSE](LICENSE) for details.
AeroSentinel Team © 2026.
