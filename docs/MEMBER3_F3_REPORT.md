# AeroSentinel — Engineering Report 02
## Feature 3, Feature 4 and Feature 5
### Hotspot Detection → PM2.5 Forecast → Evidence + Gemini WHY

> This report continues directly after Report 01 (F0–F2). It covers the three intelligence features that convert the normalized Air + Weather + H3 foundation into actionable pollution intelligence.

---

# 1. Starting Point

Before F3 begins, these must already exist:

```text
City
Air Quality
Weather
H3 Grid
PostgreSQL + PostGIS
Spring Boot REST API
React Dashboard
Python AI service
```

The intelligence pipeline now becomes:

```text
Air + Weather
      ↓
H3 Cell
      ↓
Feature Engineering
      ↓
F3 Hotspot Detection
      ↓
F4 PM2.5 Forecast
      ↓
F5 Evidence
      ↓
Gemini WHY
```

The project architecture specifies that the AI service handles feature processing, hotspot detection, forecasting, confidence, evidence reasoning and Gemini integration. The documented MVP hotspot approach allows anomaly detection, spatial/cluster logic, risk scoring, and XGBoost where supervised labels are available; the forecast approach is a baseline plus XGBoost/LightGBM-style model. LSTM/Transformer is not a Day-1 dependency.

---

# FEATURE 3 — HOTSPOT DETECTION

## 2. Goal

Identify **potential pollution hotspot H3 cells** from normalized observations and return:

```text
gridId
riskScore
riskLevel
confidence
```

Important scientific framing:

```text
Potential Hotspot
```

must be used instead of claiming that the system has proved the source of pollution.

A hotspot is a model-generated risk signal.

It is NOT:

```text
official AQI
confirmed source attribution
proof of a factory causing pollution
```

---

# 3. F3 End-to-End Flow

```text
Air Observations
Weather Observations
H3 Cell
       ↓
Feature Builder
       ↓
Feature Vector
       ↓
Hotspot Model
       ↓
Risk Score
       ↓
Risk Level
       ↓
Confidence
       ↓
Spring Boot
       ↓
Database
       ↓
React Map
```

---

# 4. F3 What the Model Actually Receives

For each H3 cell, build a structured feature vector.

Initial feature groups:

## Air features

```text
pm25_current
pm25_previous
pm25_change
pm25_rate_of_change
pm25_recent_mean
pm25_recent_max
```

## Weather features

```text
temperature
humidity
wind_speed
wind_direction
rainfall
```

## Spatial features

```text
h3_cell_id
nearby_station_distance
number_of_observations
```

The project architecture also reserves:

```text
fire_features
satellite_features
citizen_features
```

These can be plugged into the same feature contract when those layers are available.

---

# 5. F3 Feature Engineering Pipeline

```text
Raw normalized data
        ↓
Temporal alignment
        ↓
Spatial alignment
        ↓
H3 grouping
        ↓
Feature calculation
        ↓
Feature validation
        ↓
Model input
```

Example:

```text
H3-ABC

PM2.5:
09:00 → 82
10:00 → 94
11:00 → 118

Wind:
1.2 m/s

Humidity:
76%

Rain:
0

        ↓

pm25_change = +24
pm25_rate = rising
wind_speed = 1.2
humidity = 76
rainfall = 0
```

---

# 6. F3 AI Files

The target AI structure contains:

```text
ai-service/
└── app/
    ├── api/
    │   └── hotspot.py
    │
    ├── schemas/
    │   └── hotspot.py
    │
    ├── models/
    │   └── hotspot_model.py
    │
    ├── features/
    │   ├── air_features.py
    │   ├── weather_features.py
    │   └── spatial_features.py
    │
    └── services/
        └── hotspot_service.py
```

Training:

```text
ai-service/
└── training/
    └── train_hotspot.py
```

Evaluation:

```text
ai-service/
└── evaluation/
    └── hotspot_metrics.py
```

Artifacts:

```text
ai-service/
└── artifacts/
    └── models/
        └── hotspot_model.*
```

---

# 7. F3 AI Responsibilities by File

## `air_features.py`

Responsible for:

```text
PM2.5 current value
PM2.5 trend
rolling mean
rolling max
change rate
```

It must not call the database directly.

It should receive normalized data.

---

## `weather_features.py`

Responsible for:

```text
temperature
humidity
wind
rainfall
```

and derived values required by the model.

---

## `spatial_features.py`

Responsible for:

```text
H3 cell
station distance
cell-level aggregation
neighbor relationships
```

---

## `hotspot_model.py`

Responsible only for model inference.

Conceptually:

```python
features
   ↓
model.predict(...)
   ↓
risk_score
```

It should not contain:

- REST logic
- database logic
- frontend logic
- Gemini calls

---

## `hotspot_service.py`

Orchestrates:

```text
validated input
 ↓
feature processing
 ↓
model
 ↓
confidence
 ↓
structured output
```

---

# 8. F3 Model Strategy

The architecture allows two practical approaches.

## Baseline

Use:

```text
rule/anomaly/spatial risk logic
```

This is useful when labels are limited.

## Supervised model

If suitable labels are available:

```text
XGBoost / similar tree-based model
```

The goal for the hackathon is not to use the most complicated model.

The goal is:

```text
valid input
+
repeatable inference
+
reasonable evaluation
+
explainable features
```

---

# 9. F3 Confidence

Confidence must not be treated as a magical number.

It should reflect available evidence/model certainty.

Possible contributors:

```text
data completeness
recent observation density
model confidence
feature availability
distance to monitoring station
```

The exact mathematical formula must be locked by the AI team and documented.

Do not show:

```text
confidence = 98%
```

unless the model actually supports that interpretation.

---

# 10. F3 Output Contract

AI service:

```http
POST /ai/v1/hotspot/predict
```

Request:

```json
{
  "cityId": "city-pune",
  "timestamp": "2026-09-23T10:30:00Z",
  "cells": [
    {
      "h3CellId": "8928308280fffff",
      "features": {
        "pm25Current": 118.0,
        "pm25Previous": 94.0,
        "pm25Change": 24.0,
        "temperature": 28.4,
        "humidity": 71.0,
        "windSpeed": 1.8,
        "rainfall": 0.0
      }
    }
  ]
}
```

Response:

```json
{
  "modelVersion": "hotspot-v1",
  "generatedAt": "2026-09-23T10:31:00Z",
  "predictions": [
    {
      "h3CellId": "8928308280fffff",
      "riskScore": 0.84,
      "riskLevel": "HIGH",
      "confidence": 0.78
    }
  ]
}
```

---

# 11. F3 Spring Boot Files

```text
backend/src/main/java/com/aerosentinel/
└── hotspot/
    ├── HotspotController.java
    ├── HotspotService.java
    ├── HotspotPrediction.java
    ├── HotspotPredictionRepository.java
    ├── HotspotResponse.java
    └── HotspotMapper.java

backend/src/main/java/com/aerosentinel/
└── integration/
    └── ai/
        ├── AiServiceClient.java
        ├── AiServiceRequest.java
        ├── AiServiceResponse.java
        └── AiServiceException.java
```

---

# 12. F3 Backend Responsibility

Spring Boot should:

```text
receive request
      ↓
identify city/cells
      ↓
retrieve required data
      ↓
prepare AI request
      ↓
call Python
      ↓
validate AI response
      ↓
persist prediction
      ↓
return API response
```

Python should NOT become the application's main business backend.

---

# 13. F3 Database

Use:

```text
hotspot_predictions
```

Columns:

```text
id
h3_cell_id
city_id
predicted_at

risk_score
risk_level
confidence

model_version
feature_snapshot/reference

created_at
```

Relationship:

```text
City
  ↓
H3 Cell
  ↓
Hotspot Prediction
```

---

# 14. F3 Prediction Persistence

Do not overwrite old predictions blindly.

Example:

```text
10:00 → hotspot-v1 → H3-A → 0.51
10:15 → hotspot-v1 → H3-A → 0.67
10:30 → hotspot-v1 → H3-A → 0.84
```

This gives the system a prediction history and supports later event detection.

---

# 15. F3 Backend API

## Get current hotspot map

```http
GET /api/v1/hotspots?cityId=city-pune
```

Response:

```json
{
  "generatedAt": "...",
  "modelVersion": "hotspot-v1",
  "cells": [
    {
      "h3CellId": "...",
      "riskScore": 0.84,
      "riskLevel": "HIGH",
      "confidence": 0.78
    }
  ]
}
```

---

# 16. F3 Frontend Files

```text
frontend/src/
├── pages/
│   └── analyst/
│       └── HotspotPage.tsx
│
├── components/
│   └── hotspot/
│       ├── RiskCellLayer.tsx
│       ├── HotspotCard.tsx
│       ├── RiskLegend.tsx
│       └── ConfidenceBadge.tsx
│
├── services/
│   └── hotspotApi.ts
│
├── hooks/
│   └── useHotspots.ts
│
└── types/
    └── hotspot.ts
```

---

# 17. F3 Hotspot UI

```text
┌─────────────────────────────────────────────────────────────┐
│ Pollution Intelligence                                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    H3 RISK MAP                              │
│                                                             │
│              ┌───────┐ ┌───────┐                           │
│              │ MED   │ │ HIGH  │                           │
│              └───────┘ └───────┘                           │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│ Selected Cell                                               │
│                                                             │
│ Risk Score: 0.84                                            │
│ Risk Level: HIGH                                            │
│ Confidence: 0.78                                            │
│ Model: hotspot-v1                                           │
│ Generated: 10:31 AM                                         │
│                                                             │
│ [View Evidence →]                                           │
└─────────────────────────────────────────────────────────────┘
```

---

# 18. F3 Frontend State Model

The UI needs at least:

```text
LOADING
SUCCESS
EMPTY
STALE
ERROR
```

For example:

```text
Prediction generated 40 minutes ago
Status: STALE
```

rather than pretending it is current.

---

# 19. F3 Error Handling

## AI service unavailable

```text
Hotspot prediction unavailable.
Last successful prediction: 10:30 AM
```

## Invalid AI response

Spring Boot validates:

```text
riskScore
riskLevel
confidence
h3CellId
modelVersion
```

If invalid:

```text
do not persist
log error
return controlled failure
```

## Missing feature data

Do not silently substitute arbitrary values.

Return:

```text
INSUFFICIENT_DATA
```

or use a documented fallback strategy.

---

# 20. F3 Testing

### AI

- feature schema
- missing features
- model loading
- prediction range
- confidence range
- deterministic inference where expected
- model version

### Backend

- AI request mapping
- AI response mapping
- persistence
- invalid response
- timeout
- retry/failure behavior

### Frontend

- H3 risk rendering
- legend
- cell selection
- confidence display
- stale state
- empty state

---

# 21. F3 Definition of Done

- [ ] H3 cells receive features
- [ ] hotspot model runs
- [ ] risk score returned
- [ ] risk level returned
- [ ] confidence returned
- [ ] model version returned
- [ ] predictions persisted
- [ ] Spring Boot AI integration works
- [ ] React displays hotspot cells
- [ ] stale state exists
- [ ] AI failure handled
- [ ] evaluation script exists
- [ ] no causal source claim is shown

---

# FEATURE 4 — SHORT-TERM PM2.5 FORECAST

## 22. Goal

Forecast PM2.5 for the next:

```text
1–6 hours
```

The documented architecture specifies a baseline plus XGBoost/LightGBM-style approach. The forecast must remain visibly different from observed data.

---

# 23. F4 End-to-End Flow

```text
Historical PM2.5
+
Weather
+
H3 context
+
Available additional signals
        ↓
Temporal alignment
        ↓
Forecast feature engineering
        ↓
Forecast model
        ↓
1h
2h
3h
4h
5h
6h
        ↓
Confidence / interval
        ↓
Spring Boot
        ↓
Database
        ↓
React chart
```

---

# 24. F4 Forecast Features

Initial:

```text
pm25_t
pm25_t-1
pm25_t-2
rolling_mean
rolling_max
change_rate

temperature
humidity
wind_speed
wind_direction
rainfall

h3_cell
time_of_day
day_of_week
```

Future extensions:

```text
fire features
satellite features
citizen evidence
```

---

# 25. F4 AI Files

```text
ai-service/
└── app/
    ├── api/
    │   └── forecast.py
    │
    ├── schemas/
    │   └── forecast.py
    │
    ├── models/
    │   └── forecast_model.py
    │
    ├── features/
    │   └── forecast_features.py
    │
    └── services/
        └── forecast_service.py
```

Training:

```text
training/
└── train_forecast.py
```

Evaluation:

```text
evaluation/
└── forecast_metrics.py
```

---

# 26. F4 Model Responsibility

`forecast_model.py`:

```text
features
   ↓
model
   ↓
PM2.5 prediction
```

`forecast_service.py`:

```text
validation
 ↓
feature processing
 ↓
model inference
 ↓
forecast output
```

`forecast.py`:

```text
FastAPI endpoint
```

No database code should be embedded in the model file.

---

# 27. F4 Output

AI response:

```json
{
  "modelVersion": "forecast-v1",
  "generatedAt": "2026-09-23T10:30:00Z",
  "cellId": "8928308280fffff",
  "forecasts": [
    {
      "forecastFor": "2026-09-23T11:30:00Z",
      "predictedPm25": 124.0,
      "lowerBound": 112.0,
      "upperBound": 138.0,
      "confidence": 0.74
    },
    {
      "forecastFor": "2026-09-23T12:30:00Z",
      "predictedPm25": 137.0,
      "lowerBound": 119.0,
      "upperBound": 155.0,
      "confidence": 0.68
    }
  ]
}
```

The bounds are only shown if the chosen modeling method supports them.

---

# 28. F4 Database

Table:

```text
forecasts
```

Columns:

```text
id
city_id
h3_cell_id
generated_at
forecast_for

predicted_pm25
lower_bound
upper_bound
confidence

model_version
created_at
```

Relationship:

```text
H3 Cell
   ↓
Forecast
```

---

# 29. F4 Spring Boot Files

```text
backend/src/main/java/com/aerosentinel/
└── forecast/
    ├── ForecastController.java
    ├── ForecastService.java
    ├── ForecastPrediction.java
    ├── ForecastRepository.java
    ├── ForecastResponse.java
    └── ForecastMapper.java
```

AI integration remains through:

```text
integration/ai/
```

---

# 30. F4 API

```http
GET /api/v1/forecast?cityId=city-pune&cellId=...&hours=6
```

Response:

```json
{
  "cellId": "...",
  "generatedAt": "...",
  "modelVersion": "forecast-v1",
  "forecasts": [
    {
      "forecastFor": "...",
      "predictedPm25": 124,
      "lowerBound": 112,
      "upperBound": 138,
      "confidence": 0.74
    }
  ]
}
```

---

# 31. F4 Frontend Files

```text
frontend/src/
├── pages/
│   └── analyst/
│       └── ForecastPage.tsx
│
├── components/
│   └── forecast/
│       ├── ForecastChart.tsx
│       ├── ForecastCard.tsx
│       ├── ForecastConfidence.tsx
│       └── ForecastLegend.tsx
│
├── services/
│   └── forecastApi.ts
│
├── hooks/
│   └── useForecast.ts
│
└── types/
    └── forecast.ts
```

---

# 32. F4 Forecast UI

```text
┌─────────────────────────────────────────────────────────────┐
│ PM2.5 Forecast — H3-ABC                                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ PM2.5                                                      │
│ 160 ┤                                    ╱                  │
│ 140 ┤                              ╱─────                  │
│ 120 ┤                     ●───────                         │
│ 100 ┤              ●─────                                    │
│     └──────────────────────────────────────────────         │
│       Now   +1h   +2h   +3h   +4h   +5h   +6h              │
│                                                             │
│ ● Observed                                                 │
│ ─ Forecast                                                 │
│ Confidence interval shown where available                 │
└─────────────────────────────────────────────────────────────┘
```

Critical UX rule:

```text
Observed ≠ Forecast
```

Use different labels and visual treatment.

---

# 33. F4 Forecast Evaluation

Track:

```text
MAE
RMSE
forecast lead time
coverage of intervals where applicable
```

Example evaluation output:

```text
Model: forecast-v1

MAE: ...
RMSE: ...
Evaluation window: ...
Training window: ...
```

Do not claim a model is accurate without evaluation data.

---

# 34. F4 Forecast Failure Cases

## Insufficient history

```text
Forecast unavailable:
insufficient historical observations.
```

## Missing weather

Use only a documented fallback if one exists.

Otherwise:

```text
Forecast confidence reduced / unavailable
```

## AI service unavailable

Display last successful forecast with timestamp and stale status.

## Forecast model unavailable

Return:

```text
MODEL_UNAVAILABLE
```

Do not generate fake forecast numbers.

---

# 35. F4 Definition of Done

- [ ] 1–6h forecast endpoint works
- [ ] forecast model version stored
- [ ] generated_at stored
- [ ] forecast_for stored
- [ ] observed vs forecast separated
- [ ] confidence shown where supported
- [ ] evaluation metrics calculated
- [ ] React chart works
- [ ] missing-history case handled
- [ ] AI failure handled
- [ ] stale forecast handled

---

# FEATURE 5 — EVIDENCE + GEMINI WHY

## 36. Goal

F5 makes the model output understandable.

The project explicitly requires observed signals and model-derived explanation to be shown separately.

The system should answer:

```text
WHY is this H3 cell considered a potential hotspot?
```

without pretending Gemini is the numerical prediction model.

---

# 37. F5 Core Principle

There are two different things:

## Observed Evidence

Facts from data:

```text
PM2.5 increased from 94 to 118
Wind speed = 1.8 m/s
Humidity = 71%
Forecast +2h = 137
```

## Model Interpretation

```text
The combination of recent PM2.5 increase and low wind
is consistent with elevated pollution risk.
```

Gemini should explain the structured evidence.

It must not invent evidence.

---

# 38. F5 End-to-End Flow

```text
Hotspot Prediction
       +
Forecast
       +
Air Data
       +
Weather Data
       ↓
Evidence Builder
       ↓
Structured Evidence
       ↓
Gemini Prompt
       ↓
Gemini
       ↓
Structured Explanation
       ↓
Spring Boot
       ↓
Database
       ↓
React Evidence Panel
```

---

# 39. F5 Evidence Object

Example:

```json
{
  "h3CellId": "8928308280fffff",
  "riskScore": 0.84,
  "confidence": 0.78,
  "signals": [
    {
      "type": "PM25_TREND",
      "value": "25.5% increase",
      "source": "Air observation",
      "observedAt": "..."
    },
    {
      "type": "WIND",
      "value": "1.8 m/s",
      "source": "Weather",
      "observedAt": "..."
    },
    {
      "type": "FORECAST",
      "value": "137 ug/m3 in 2h",
      "source": "Forecast model",
      "observedAt": "..."
    }
  ]
}
```

---

# 40. F5 Backend Files

```text
backend/src/main/java/com/aerosentinel/
└── evidence/
    ├── EvidenceController.java
    ├── EvidenceService.java
    ├── EvidenceRecord.java
    ├── EvidenceRepository.java
    ├── EvidenceResponse.java
    └── EvidenceBuilder.java
```

Gemini integration:

```text
backend/src/main/java/com/aerosentinel/
└── integration/
    └── ai/
        ├── GeminiClient.java
        ├── GeminiPromptBuilder.java
        ├── GeminiResponseParser.java
        └── GeminiException.java
```

---

# 41. F5 AI Service Files

```text
ai-service/
└── app/
    ├── api/
    │   ├── evidence.py
    │   └── gemini.py
    │
    ├── schemas/
    │   └── evidence.py
    │
    ├── services/
    │   ├── evidence_service.py
    │   └── gemini_service.py
    │
    └── utils/
        └── prompt_templates.py
```

---

# 42. Gemini Responsibility

Gemini receives:

```text
validated structured evidence
+
model output
```

and produces:

```text
human-readable explanation
```

Gemini does NOT produce:

```text
riskScore
PM2.5 measurement
official AQI
causal source attribution
```

Those come from the relevant data/model systems.

---

# 43. Gemini Prompt Contract

Conceptual prompt:

```text
You are explaining an environmental risk prediction.

Use ONLY the supplied structured evidence.

Requirements:
1. Separate observed facts from interpretation.
2. Do not invent measurements.
3. Do not claim causal source attribution.
4. State uncertainty where relevant.
5. Use concise authority-facing language.
6. Refer to the location as a potential hotspot.
7. Do not call the output official AQI.
```

Input:

```json
{
  "riskLevel": "HIGH",
  "riskScore": 0.84,
  "confidence": 0.78,
  "observedSignals": [...],
  "forecastSignals": [...]
}
```

---

# 44. Gemini Output Contract

Prefer structured JSON.

Example:

```json
{
  "summary": "This cell is classified as a potential high-risk pollution hotspot.",
  "observedEvidence": [
    "PM2.5 increased by 25.5% in the recent window.",
    "Wind speed is 1.8 m/s."
  ],
  "modelInterpretation": [
    "Recent PM2.5 increase combined with low wind is consistent with elevated local pollution risk."
  ],
  "uncertainty": "Confidence is moderate because monitoring coverage is limited.",
  "recommendedVerification": "Field verification is recommended."
}
```

The backend validates this response before storing/displaying it.

---

# 45. F5 Database

## `event_evidence` or `evidence_records`

The documented database model includes:

```text
event_evidence
```

For the intelligence stage, evidence should be linkable to the prediction/H3 context.

Suggested fields:

```text
id
h3_cell_id
prediction_id
evidence_type
value
unit
source
observed_at
created_at
```

---

## `gemini_analyses`

The documented core database model includes:

```text
gemini_analyses
```

Fields:

```text
id
target_type
target_id
prompt_version
model
analysis_json
generated_at
status
```

Keep the raw structured Gemini response separately from the UI rendering logic.

---

# 46. F5 Frontend Files

```text
frontend/src/
├── components/
│   └── evidence/
│       ├── EvidencePanel.tsx
│       ├── SignalList.tsx
│       ├── ObservedSignals.tsx
│       ├── ModelInterpretation.tsx
│       ├── WhyExplanation.tsx
│       └── UncertaintyNotice.tsx
│
├── services/
│   └── evidenceApi.ts
│
├── hooks/
│   └── useEvidence.ts
│
└── types/
    └── evidence.ts
```

---

# 47. F5 UI Wireframe

```text
┌──────────────────────────────────────────────────────────────┐
│ Potential Hotspot — H3-ABC                                  │
├──────────────────────────────────────────────────────────────┤
│ Risk Score        0.84                                      │
│ Risk Level        HIGH                                      │
│ Confidence        0.78                                      │
├──────────────────────────────────────────────────────────────┤
│ OBSERVED SIGNALS                                            │
│                                                              │
│ ● PM2.5 increased 25.5%                                     │
│ ● Wind speed: 1.8 m/s                                      │
│ ● Humidity: 71%                                             │
│ ● Forecast at +2h: 137 μg/m³                               │
├──────────────────────────────────────────────────────────────┤
│ WHY?                                                        │
│                                                              │
│ Recent PM2.5 increase combined with low wind is             │
│ consistent with elevated local pollution risk.              │
│                                                              │
│ Uncertainty: Moderate                                       │
│                                                              │
│ Field verification recommended.                             │
└──────────────────────────────────────────────────────────────┘
```

---

# 48. F5 Evidence Separation

This is mandatory.

### Section A

```text
OBSERVED
```

Only data.

### Section B

```text
MODEL OUTPUT
```

Risk score / forecast.

### Section C

```text
AI INTERPRETATION
```

Gemini explanation.

### Section D

```text
RECOMMENDED VERIFICATION
```

Operational recommendation.

Never mix them into one paragraph.

---

# 49. F5 Gemini Failure Handling

## Gemini timeout

Display:

```text
AI explanation temporarily unavailable.
```

But still show:

```text
Risk score
Confidence
Observed signals
Forecast
```

The core intelligence must remain useful without Gemini.

---

## Gemini invalid JSON

```text
Gemini response validation failed.
```

Do not display malformed model output as authoritative.

---

## Gemini hallucination prevention

Backend should only send structured values.

Example:

Bad:

```text
"Tell me why this factory is polluting."
```

Good:

```json
{
  "h3CellId": "...",
  "pm25Change": 25.5,
  "windSpeed": 1.8,
  "forecast2h": 137
}
```

Then ask Gemini to interpret ONLY those facts.

---

# 50. F5 Security

Gemini API key:

```text
Backend / AI service environment
```

Never:

```text
React source
frontend .env exposed to browser
Git
```

Also:

- sanitize user-controlled evidence text
- limit prompt size
- validate image/text inputs later in F6
- log prompt version, not sensitive secrets

---

# 51. F5 Testing

## Evidence

Test:

- correct signals collected
- timestamps preserved
- sources preserved
- no duplicate signals
- observed/model distinction

## Gemini

Test:

- valid structured response
- malformed response
- timeout
- empty response
- unsupported claim detection where feasible
- prompt version

## Frontend

Test:

- observed signals render
- model output renders
- explanation renders
- uncertainty renders
- Gemini unavailable state
- stale analysis state

---

# 52. F5 Definition of Done

- [ ] Evidence builder works
- [ ] Air evidence available
- [ ] Weather evidence available
- [ ] Forecast evidence available
- [ ] Hotspot output linked
- [ ] Gemini receives structured evidence
- [ ] Gemini explanation is structured
- [ ] Observed facts separated from interpretation
- [ ] Uncertainty displayed
- [ ] Prompt version stored
- [ ] Gemini failure does not break hotspot UI
- [ ] No causal source claim
- [ ] React evidence panel works

---

# 53. Combined Architecture After F3–F5

The system now becomes:

```text
                    AIR
                     │
                   WEATHER
                     │
                     ▼
                  H3 CELL
                     │
             Feature Engineering
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
   HOTSPOT MODEL          FORECAST MODEL
          │                     │
          │                     │
          └──────────┬──────────┘
                     ▼
                  EVIDENCE
                     │
                     ▼
                  GEMINI
                     │
                     ▼
                WHY / BRIEF
                     │
                     ▼
              Frontend Analyst UI
```

---

# 54. Cross-Feature Database Relationships

```text
City
 │
 └── H3 Cell
       │
       ├── Air Observations
       │
       ├── Weather Observations
       │
       ├── Hotspot Predictions
       │
       ├── Forecasts
       │
       └── Evidence
              │
              └── Gemini Analysis
```

The important design decision is that H3 remains the shared spatial key.

---

# 55. Cross-Feature API Relationships

```text
GET /cities/{cityId}/air-quality
              │
              ▼
         Feature Builder
              │
              ▼
POST /ai/v1/hotspot/predict
              │
              ▼
GET /hotspots
              │
              ▼
POST /ai/v1/forecast
              │
              ▼
GET /forecast
              │
              ▼
GET /evidence/{targetId}
              │
              ▼
POST /ai/v1/gemini/explain
```

The exact internal routing can be implemented differently, but the contracts must remain explicit.

---

# 56. Team Split

## Member 1 — Backend

### F3

```text
HotspotController
HotspotService
Prediction persistence
AI client
```

### F4

```text
ForecastController
ForecastService
Forecast persistence
AI client
```

### F5

```text
EvidenceBuilder
Evidence persistence
Gemini client
Gemini response validation
```

---

## Member 2 — Frontend

### F3

```text
Risk map
Hotspot cards
Risk legend
Confidence
```

### F4

```text
Forecast page
Forecast chart
Observed vs forecast
Confidence
```

### F5

```text
Evidence panel
Observed signals
WHY
Uncertainty
AI status
```

---

## Member 3 — AI/ML/Data/GIS

### F3

```text
Feature engineering
Hotspot model
Confidence
Training
Evaluation
```

### F4

```text
Forecast features
Forecast model
Training
MAE/RMSE
```

### F5

```text
Evidence reasoning
Gemini prompts
Structured Gemini response
Grounding rules
```

---

# 57. Parallel Development Strategy

The teams do NOT need to wait for each other.

## F3

```text
Member 3
  ↓
FastAPI contract + sample prediction

Member 1
  ↓
Spring Boot AI client + persistence

Member 2
  ↓
Mock hotspot API + risk map
```

Then:

```text
Real AI
   ↓
Spring Boot
   ↓
React
```

---

## F4

While F3 is being integrated:

```text
Member 3 → forecast model
Member 1 → forecast API/database
Member 2 → forecast chart
```

---

## F5

In parallel:

```text
Member 3 → Gemini prompt + structured response
Member 1 → evidence aggregation + Gemini client
Member 2 → evidence panel
```

---

# 58. Integration Gates

## Gate 1 — F3

Must work:

```text
H3 cell
 ↓
features
 ↓
model
 ↓
risk
 ↓
database
 ↓
map
```

## Gate 2 — F4

Must work:

```text
H3 cell
 ↓
history
 ↓
forecast
 ↓
database
 ↓
chart
```

## Gate 3 — F5

Must work:

```text
Hotspot
 ↓
Evidence
 ↓
Gemini
 ↓
WHY
```

Only after all three work should the team start F6.

---

# 59. End-to-End Demo After F5

A strong demo flow is:

```text
1. Select Pune
       ↓
2. Select H3 cell
       ↓
3. Show current PM2.5
       ↓
4. Show weather
       ↓
5. Show potential hotspot
       ↓
6. Show risk score + confidence
       ↓
7. Open forecast
       ↓
8. Show 1–6h PM2.5 forecast
       ↓
9. Open Evidence
       ↓
10. Show observed signals
       ↓
11. Show Gemini WHY
       ↓
12. Show uncertainty
       ↓
13. Field verification recommended
```

This creates the first complete intelligence story before citizen reporting and authority workflow are added.

---

# 60. Engineering Rules for F3–F5

1. Do not call risk score official AQI.
2. Do not claim the model proves causation.
3. Do not let Gemini generate numerical pollution predictions.
4. Do not hide model confidence.
5. Do not mix observed measurements with AI-generated interpretation.
6. Do not display stale predictions as live.
7. Do not overwrite prediction history without a documented reason.
8. Store model versions.
9. Store generation timestamps.
10. Validate AI responses before persistence.
11. Do not silently fabricate missing features.
12. Keep the H3 identifier consistent across all intelligence features.
13. Keep provider-specific formats outside the domain layer.
14. Keep AI models independent from REST/database code.
15. Keep Gemini failure independent from core numerical prediction availability.

---

# 61. Final Completion Checklist

## F3 — Hotspot

- [ ] feature engineering
- [ ] model
- [ ] confidence
- [ ] FastAPI endpoint
- [ ] Spring Boot client
- [ ] database
- [ ] REST API
- [ ] risk map
- [ ] evaluation
- [ ] failure handling

## F4 — Forecast

- [ ] temporal features
- [ ] model
- [ ] 1–6h output
- [ ] optional interval
- [ ] confidence
- [ ] evaluation
- [ ] database
- [ ] API
- [ ] chart
- [ ] stale state
- [ ] failure handling

## F5 — Evidence + Gemini

- [ ] evidence builder
- [ ] observed signals
- [ ] model signals
- [ ] structured prompt
- [ ] Gemini service
- [ ] response validation
- [ ] analysis persistence
- [ ] WHY panel
- [ ] uncertainty
- [ ] fallback when Gemini fails
- [ ] no causal claims

---

# 62. Final State

After F3–F5:

```text
             DATA LAYER
                 │
        Air + Weather + H3
                 │
                 ▼
        ┌────────────────┐
        │ HOTSPOT MODEL  │
        └───────┬────────┘
                │
                ├───────────────┐
                │               │
                ▼               ▼
          Risk Score        Confidence
                │
                ▼
        ┌────────────────┐
        │ FORECAST MODEL │
        └───────┬────────┘
                │
                ▼
          1–6h Forecast
                │
                ▼
        ┌────────────────┐
        │ EVIDENCE ENGINE│
        └───────┬────────┘
                │
                ▼
             Gemini
                │
                ▼
          WHY / Explanation
                │
                ▼
          Analyst Dashboard
```

At this point AeroSentinel has moved from a simple monitoring dashboard to an **intelligence pipeline**:

```text
Observe
  ↓
Spatialize
  ↓
Detect
  ↓
Forecast
  ↓
Explain
```

The next report should cover:

```text
F6 — Citizen Report + Gemini Vision
F7 — Pollution Event + Authority Alert
F8 — Monitoring Gap + Sensor Recommendation
```

Those three features will connect the intelligence layer to the actual **action workflow**.
