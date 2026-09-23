# AeroSentinel — Product Requirements Document (PRD)

**Version:** 1.0  
**Status:** Scope Locked  
**Track:** Clean Air & Climate Resilience  

---

## 1. Executive Summary & Vision

AeroSentinel is an **AI-powered federated hyperlocal pollution intelligence and early-action platform**. While Indian metropolitan areas maintain networks of Continuous Ambient Air Quality Monitoring Stations (CAAQMS), these fixed stations are spatially sparse and cannot monitor every neighborhood continuously. Ephemeral and localized events—such as municipal solid waste burning, agricultural residue fires, localized vehicular congestion, and unauthorized industrial emissions—frequently emerge in the unmonitored zones between stations.

AeroSentinel fuses:
- **Ground Sensor Measurements**: CAAQMS observations (PM2.5, PM10, NO2, SO2, CO) from CPCB and OpenAQ.
- **Meteorological Data**: Temperature, relative humidity, wind speed, wind direction, boundary layer dynamics.
- **Satellite Atmospheric Indicators**: Sentinel-5P tropospheric NO2/SO2 column densities.
- **Active Fire Detections**: NASA FIRMS VIIRS/MODIS thermal anomalies.
- **Crowdsourced Citizen Evidence**: Geo-located smartphone photographs and incident reports.

By indexing these heterogeneous signals into fine-grained **Uber H3 hexagonal spatial cells**, AeroSentinel detects emerging pollution hotspots, forecasts short-term (1–6 hour) PM2.5 spikes, explains the underlying multi-source evidence using **Google Gemini**, and provides municipal environmental protection authorities with an actionable incident queue for field inspection and enforcement.

---

## 2. Problem Statement & Scope Boundaries

### The Core Problem
1. **Spatial Monitoring Gaps**: Fixed monitoring stations are located 5 to 20 km apart, leaving large residential and industrial pockets unmonitored.
2. **Delayed Visibility**: Standard air-quality reporting publishes 1-hour or 24-hour historical averages, failing to catch rapid, localized pollution spikes before citizens are exposed.
3. **Fragmented Environmental Data**: Satellite data, weather forecasts, fire telemetry, and ground observations reside in separate silos without spatial unification.
4. **Lack of Actionable Evidence**: Authorities lack automated triage to determine why a localized spike occurred, which team should inspect it, and where temporary mobile sensors should be deployed.

### What AeroSentinel Is NOT
- **Not a Replacement for CAAQMS**: AeroSentinel complements official networks; it does not claim to replace regulatory-grade reference stations.
- **Not Causal Proof of Specific Source**: While correlations with fires, roads, and industrial zones are surfaced as evidence, AeroSentinel flags *potential* hotspots and recommends physical verification rather than making legal determinations.
- **Satellite Columns $\neq$ Ground PM2.5**: Satellite column densities are treated as atmospheric indicators, not direct surface concentrations.
- **Gemini is Not the Numerical Predictor**: Numerical predictions (risk scores, PM2.5 concentrations) are produced by statistical and gradient-boosted ML models; Gemini provides multimodal photo analysis and qualitative reasoning over validated model outputs.

---

## 3. Target User Personas

1. **Municipal Environmental Officers / Pollution Control Boards (SPCBs)**:
   - Needs: Early detection of localized spikes, prioritized task queues, team dispatch, and inspection logging.
2. **Field Verification & Mobile Sensor Teams**:
   - Needs: Precise spatial guidance (H3 hex coordinates), routing to high-uncertainty hotspots, sensor deployment tracking.
3. **Environmental Analysts & Data Scientists**:
   - Needs: Multi-parameter time-series charts, feature importance metrics, model confidence intervals, and spatial layer overlays.
4. **Citizens & Community Watchdogs**:
   - Needs: Simple localized air quality views, short-term hourly advisories, and an easy photo-reporting interface.

---

## 4. System Capabilities & Feature Requirements

### FR-1: Hyperlocal Spatial Grid (H3)
- Spatial partitioning using Uber H3 hexagonal indexing (resolutions 7 to 9, ~1.2 km to ~170 m edge lengths).
- Aggregation of all sensor observations, weather, fire detections, and citizen reports into their respective H3 cells.

### FR-2: Hotspot Risk Detection
- Continuous evaluation of grid feature vectors (PM2.5 trend, wind dispersion, fire proximity, citizen complaints).
- Scoring of each cell on a risk scale: `LOW`, `MEDIUM`, `HIGH`.

### FR-3: Short-Term PM2.5 Forecasting
- Rolling 1 to 6-hour PM2.5 concentration forecasts per H3 cell with confidence bounds.

### FR-4: Gemini Multimodal Evidence Explanation
- Vision analysis of citizen photos to detect smoke, open waste burning, construction dust, or industrial plumes.
- Natural language explanation of multi-source evidence ("Why is this cell flagged high risk?").

### FR-5: Incident Lifecycle & Authority Queue
- Automated alert generation when hotspot risk exceeds threshold.
- Status workflow: `OPEN` $\rightarrow$ `ASSIGNED` $\rightarrow$ `FIELD_INSPECTION` $\rightarrow$ `ACTION_TAKEN` $\rightarrow$ `RESOLVED`.

### FR-6: Monitoring Coverage Optimization
- Calculation of monitoring priority based on cell risk, prediction uncertainty, and Euclidean/geodesic distance to nearest CAAQMS.
- Recommending coordinates for mobile sensor placement.

### FR-7: Multi-City Federated Learning Prototype
- Demonstration of distributed local model training across Pune, Mumbai, and Delhi nodes.
- Model parameter aggregation by a central coordinator without sharing raw city observations.
