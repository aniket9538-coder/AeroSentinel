# AeroSentinel — System Architecture

**Document Version:** 1.0  
**Scope:** Core Architecture Specification  

---

## 1. High-Level Architecture Overview

AeroSentinel follows a multi-tier microservices-aligned architecture designed for resilience, clean separation of concerns, and spatial data scalability:

```text
 ┌────────────────────────────────────────────────────────┐
 │                   PRESENTATION TIER                    │
 │               React 18 + TypeScript + Vite             │
 │                                                        │
 │  [Public Views]   [Analyst Portal]   [Authority Hub]   │
 │   - Map View       - Hotspot Matrix   - Incident Queue │
 │   - Citizen Form   - Forecast Detail  - Field Actions  │
 └───────────────────────────┬────────────────────────────┘
                             │ HTTPS / REST (JSON)
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │                 CORE APPLICATION TIER                  │
 │              Java 21 + Spring Boot 3.3.x               │
 │                                                        │
 │  - System Orchestrator & API Gateway                   │
 │  - Authentication (JWT & Spring Security)              │
 │  - Pollution Event & Alert Lifecycle Manager           │
 │  - Authority Task & Inspection Dispatcher              │
 │  - Monitoring Coverage Index Calculator                │
 │  - External Ingestion Adapters (CPCB, IMD, FIRMS)      │
 │  - Spatial Data Utilities (Uber H3 Indexing)           │
 └─────────────┬───────────────────────────┬──────────────┘
               │ JDBC / JPA (PostGIS)      │ Internal HTTP / REST
               ▼                           ▼
 ┌───────────────────────────┐ ┌──────────────────────────┐
 │      PERSISTENCE TIER     │ │     AI / ML SERVICE      │
 │   PostgreSQL 16 + PostGIS │ │      Python FastAPI      │
 │                           │ │                          │
 │ - Spatio-Temporal Tables  │ │ - Feature Engineering    │
 │ - H3 Cell Aggregations    │ │ - ML Hotspot Risk Scorer │
 │ - Incident Audit Trails   │ │ - 1–6h PM2.5 Forecaster  │
 │ - Reference Infrastructure│ │ - Gemini Explanation     │
 └───────────────────────────┘ │ - Gemini Vision Service  │
                               └───────────┬──────────────┘
                                           │ HTTPS (Server-side)
                                           ▼
                               ┌──────────────────────────┐
                               │   Google Gemini API      │
                               │  (Multimodal Reasoning)  │
                               └──────────────────────────┘
```

---

## 2. Component Responsibilities

### 2.1 Frontend Tier (`frontend/`)
- Built using **React 18**, **TypeScript**, and **Vite**.
- **Map Visualization**: Utilizes **Leaflet** and **React-Leaflet** with custom H3 polygon layers rendered via **h3-js**.
- **Charting**: Provides time-series pollutant curves, weather overlays, and confidence bands using **Recharts**.
- **Role-Based Experience**:
  - `Public`: Hyperlocal AQI check, 6-hour forecast, geo-located citizen reporting.
  - `Analyst`: Multi-station comparison, correlation analysis, satellite indicator overlays.
  - `Authority`: Real-time incident triage, inspection scheduling, mitigation logging.
  - `Federated`: Node health, round metrics, and global model convergence monitoring.

### 2.2 Core Application Tier (`backend/`)
- Built using **Java 21** and **Spring Boot 3.3.x**.
- **Primary Orchestrator**: Manages state machines, schedules ingestion pipelines, enforces role-based access control (RBAC), and handles data persistence.
- **Hibernate Spatial / PostGIS**: Manages geometric types (Point, Polygon) alongside H3 hex index strings.
- **Integration Clients**: Outbound HTTP clients calling external APIs (CPCB, IMD, NASA FIRMS) and internal Python AI endpoints.
- **Fail-Safe Operation**: If external or AI services are unavailable, the backend gracefully serves cached or synthetic baseline models.

### 2.3 AI / ML Service (`ai-service/`)
- Built using **Python 3.11+**, **FastAPI**, and **Pydantic**.
- **Feature Pipeline**: Combines pollutant lags, rolling statistics, wind direction vectors, fire proximity, and citizen signal density.
- **Hotspot Classifier**: Gradient-boosted trees (XGBoost) and anomaly detection identifying high-risk cells.
- **Forecaster**: Short-term regressor predicting $t+1$ to $t+6$ hour PM2.5 values.
- **Gemini Service**: Server-side bridge to Google GenAI for photo classification and structured evidence synthesis.

### 2.4 Federated Prototype (`federated/`)
- Standalone multi-node simulation for Pune, Mumbai, and Delhi.
- City clients compute local gradient updates on partitioned datasets and transmit weight deltas to the central coordinator.
- The coordinator performs Federated Averaging (FedAvg) and distributes updated global model weights.

---

## 3. Communication Boundaries & Protocols

| Interaction | Protocol | Payload | Security |
|---|---|---|---|
| Frontend $\leftrightarrow$ Backend | HTTPS | JSON / Multipart | JWT Bearer Token |
| Backend $\leftrightarrow$ Database | TCP / JDBC | SQL / PostGIS Geometry | TLS, Internal Network |
| Backend $\leftrightarrow$ AI Service | HTTP | JSON (Pydantic models) | VPC / Internal Docker Network |
| AI Service $\leftrightarrow$ Gemini API | HTTPS | JSON (Official GenAI SDK) | Server-Side API Key |
| Frontend $\leftrightarrow$ Database | **Prohibited** | N/A | Strictly blocked by architecture |
| Frontend $\leftrightarrow$ AI Service | **Prohibited** | N/A | All traffic routed via Spring Boot |
