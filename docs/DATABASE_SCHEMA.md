# AeroSentinel — Database Schema (PostgreSQL + PostGIS)

---

## 1. Relational & Spatial Entity Model

```text
               ┌─────────────┐
               │    CITIES   │
               └──────┬──────┘
       ┌──────────────┼───────────────────────────┬──────────────────┐
       │              │                           │                  │
       ▼              ▼                           ▼                  ▼
┌──────────────┐┌──────────────┐          ┌──────────────┐    ┌──────────────┐
│  MONITORING  ││   WEATHER    │          │  FIRE_EVENTS │    │  SATELLITE_  │
│   STATIONS   ││ OBSERVATIONS │          └──────────────┘    │ OBSERVATIONS │
└──────┬───────┘└──────────────┘                              └──────────────┘
       │
       ▼
┌──────────────┐
│     AIR_     │
│ OBSERVATIONS │
└──────────────┘

       ┌──────────────┐                   ┌──────────────┐
       │  GRID_CELLS  │ (H3 Index)        │  APP_USERS   │
       └──────┬───────┘                   └──────┬───────┘
       ┌──────┼────────────────┐                 │
       │      │                │                 │
       ▼      ▼                ▼                 ▼
┌──────────┐┌─────────────┐┌───────────┐  ┌──────────────┐
│  GRID_   ││   HOTSPOT_   ││ FORECASTS│  │   CITIZEN_   │
│ FEATURES ││ PREDICTIONS │└───────────┘  │   REPORTS    │
└──────────┘└──────┬──────┘               └──────┬───────┘
                   │                             │
                   ▼                             ▼
       ┌────────────────────────┐         ┌──────────────┐
       │    POLLUTION_EVENTS    │◄────────┤    GEMINI_   │
       └───────────┬────────────┘         │   ANALYSES   │
                   │                      └──────────────┘
                   ▼
       ┌────────────────────────┐
       │    EVENT_EVIDENCE      │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │         ALERTS         │
       └───────────┬────────────┘
       ┌───────────┴────────────┐
       ▼                        ▼
┌──────────────┐         ┌──────────────┐
│  AUTHORITY_  │         │ INSPECTIONS  │
│   ACTIONS    │         └──────────────┘
└──────────────┘

┌────────────────────────────────────────────────────────┐
│ FEDERATED LEARNING:  FEDERATED_NODES ──► MODEL_UPDATES  │
└────────────────────────────────────────────────────────┘
```

---

## 2. Table Specifications

### 2.1 Core Geographies & Stations

- **`cities`**:
  - `id` UUID PRIMARY KEY
  - `name` VARCHAR(100) NOT NULL
  - `state` VARCHAR(100) NOT NULL
  - `country` VARCHAR(100) DEFAULT 'India'
  - `timezone` VARCHAR(50) DEFAULT 'Asia/Kolkata'
  - `latitude` DOUBLE PRECISION NOT NULL
  - `longitude` DOUBLE PRECISION NOT NULL
  - `active` BOOLEAN DEFAULT TRUE
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`monitoring_stations`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `station_code` VARCHAR(50) UNIQUE NOT NULL
  - `name` VARCHAR(150) NOT NULL
  - `agency` VARCHAR(100) DEFAULT 'CPCB'
  - `latitude` DOUBLE PRECISION NOT NULL
  - `longitude` DOUBLE PRECISION NOT NULL
  - `location` GEOGRAPHY(Point, 4326)
  - `status` VARCHAR(30) DEFAULT 'ACTIVE'
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

### 2.2 Observational Telemetry

- **`air_observations`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `station_id` VARCHAR(50) NOT NULL
  - `latitude` DOUBLE PRECISION NOT NULL
  - `longitude` DOUBLE PRECISION NOT NULL
  - `location` GEOGRAPHY(Point, 4326)
  - `observed_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `pm25`, `pm10`, `no2`, `so2`, `co`, `o3`, `aqi` DOUBLE PRECISION
  - `source` VARCHAR(50) DEFAULT 'CPCB'
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`weather_observations`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `latitude` DOUBLE PRECISION NOT NULL
  - `longitude` DOUBLE PRECISION NOT NULL
  - `observed_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `temperature`, `humidity`, `wind_speed`, `wind_direction`, `rainfall`, `pressure` DOUBLE PRECISION
  - `source` VARCHAR(50)
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`fire_events`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `latitude` DOUBLE PRECISION NOT NULL
  - `longitude` DOUBLE PRECISION NOT NULL
  - `location` GEOGRAPHY(Point, 4326)
  - `detected_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `confidence` DOUBLE PRECISION
  - `frp` DOUBLE PRECISION
  - `satellite` VARCHAR(50)
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`satellite_observations`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `h3_index` VARCHAR(30) NOT NULL
  - `observed_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `no2_value`, `so2_value`, `aerosol_indicator` DOUBLE PRECISION
  - `source_product` VARCHAR(100)
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

### 2.3 Spatial Grid & Model Intelligence

- **`grid_cells`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `h3_index` VARCHAR(30) UNIQUE NOT NULL
  - `resolution` INTEGER NOT NULL DEFAULT 8
  - `center_latitude` DOUBLE PRECISION NOT NULL
  - `center_longitude` DOUBLE PRECISION NOT NULL
  - `boundary` GEOGRAPHY(Polygon, 4326)
  - `active` BOOLEAN DEFAULT TRUE

- **`grid_features`**:
  - `id` UUID PRIMARY KEY
  - `grid_cell_id` UUID REFERENCES grid_cells(id)
  - `feature_time` TIMESTAMP WITH TIME ZONE NOT NULL
  - `pm25_current`, `pm25_lag_1h`, `pm25_lag_3h`, `pm25_trend` DOUBLE PRECISION
  - `temperature`, `humidity`, `wind_speed`, `wind_direction` DOUBLE PRECISION
  - `fire_count` INTEGER DEFAULT 0
  - `nearest_fire_distance` DOUBLE PRECISION
  - `nearest_station_distance` DOUBLE PRECISION
  - `citizen_report_count` INTEGER DEFAULT 0
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`hotspot_predictions`**:
  - `id` UUID PRIMARY KEY
  - `grid_cell_id` UUID REFERENCES grid_cells(id)
  - `predicted_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `risk_score` DOUBLE PRECISION NOT NULL
  - `risk_level` VARCHAR(20) NOT NULL
  - `confidence` DOUBLE PRECISION NOT NULL
  - `model_version` VARCHAR(50) NOT NULL
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`forecasts`**:
  - `id` UUID PRIMARY KEY
  - `grid_cell_id` UUID REFERENCES grid_cells(id)
  - `generated_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `target_time` TIMESTAMP WITH TIME ZONE NOT NULL
  - `horizon_hours` INTEGER NOT NULL
  - `predicted_pm25` DOUBLE PRECISION NOT NULL
  - `lower_bound`, `upper_bound`, `confidence` DOUBLE PRECISION
  - `model_version` VARCHAR(50) NOT NULL

### 2.4 Citizen Evidence & Multimodal Analysis

- **`citizen_reports`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `latitude` DOUBLE PRECISION NOT NULL
  - `longitude` DOUBLE PRECISION NOT NULL
  - `location` GEOGRAPHY(Point, 4326)
  - `h3_index` VARCHAR(30)
  - `category` VARCHAR(50) NOT NULL
  - `description` TEXT
  - `image_url` VARCHAR(500)
  - `submitted_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()
  - `status` VARCHAR(30) DEFAULT 'PENDING'

- **`gemini_analyses`**:
  - `id` UUID PRIMARY KEY
  - `citizen_report_id` UUID REFERENCES citizen_reports(id)
  - `model_name` VARCHAR(50)
  - `detected_category` VARCHAR(50)
  - `confidence` DOUBLE PRECISION
  - `narrative_summary` TEXT
  - `evidence_corroborated` BOOLEAN DEFAULT FALSE
  - `raw_response` JSONB
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

### 2.5 Pollution Events & Authority Action

- **`pollution_events`**:
  - `id` UUID PRIMARY KEY
  - `grid_cell_id` UUID REFERENCES grid_cells(id)
  - `event_code` VARCHAR(50) UNIQUE NOT NULL
  - `severity` VARCHAR(20) NOT NULL
  - `status` VARCHAR(30) DEFAULT 'OPEN'
  - `started_at` TIMESTAMP WITH TIME ZONE NOT NULL
  - `resolved_at` TIMESTAMP WITH TIME ZONE

- **`event_evidence`**:
  - `id` UUID PRIMARY KEY
  - `event_id` UUID REFERENCES pollution_events(id)
  - `source_type` VARCHAR(50) NOT NULL
  - `evidence_key` VARCHAR(100) NOT NULL
  - `evidence_value` TEXT NOT NULL
  - `weight` DOUBLE PRECISION DEFAULT 1.0

- **`alerts`**:
  - `id` UUID PRIMARY KEY
  - `event_id` UUID REFERENCES pollution_events(id)
  - `grid_cell_id` UUID REFERENCES grid_cells(id)
  - `severity` VARCHAR(20) NOT NULL
  - `title` VARCHAR(200) NOT NULL
  - `message` TEXT NOT NULL
  - `status` VARCHAR(30) DEFAULT 'OPEN'
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

- **`inspections`**:
  - `id` UUID PRIMARY KEY
  - `alert_id` UUID REFERENCES alerts(id)
  - `assigned_team` VARCHAR(100) NOT NULL
  - `scheduled_at` TIMESTAMP WITH TIME ZONE
  - `findings` TEXT
  - `status` VARCHAR(30) DEFAULT 'SCHEDULED'

- **`authority_actions`**:
  - `id` UUID PRIMARY KEY
  - `alert_id` UUID REFERENCES alerts(id)
  - `action_type` VARCHAR(100) NOT NULL
  - `action_details` TEXT NOT NULL
  - `performed_by` VARCHAR(100) NOT NULL
  - `performed_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()

### 2.6 Federated Prototype

- **`federated_nodes`**:
  - `id` UUID PRIMARY KEY
  - `city_id` UUID REFERENCES cities(id)
  - `node_name` VARCHAR(100) NOT NULL
  - `status` VARCHAR(30) DEFAULT 'ONLINE'
  - `model_version` VARCHAR(50)

- **`model_updates`**:
  - `id` UUID PRIMARY KEY
  - `node_id` UUID REFERENCES federated_nodes(id)
  - `round_number` INTEGER NOT NULL
  - `sample_count` INTEGER NOT NULL
  - `metrics` JSONB
  - `created_at` TIMESTAMP WITH TIME ZONE DEFAULT NOW()
