-- AeroSentinel Schema Definition (V1)
-- Compatible with PostgreSQL 16 + PostGIS 3.4

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- 1. CITIES
CREATE TABLE IF NOT EXISTS cities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) DEFAULT 'India',
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. MONITORING STATIONS
CREATE TABLE IF NOT EXISTS monitoring_stations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    station_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    agency VARCHAR(100) DEFAULT 'CPCB',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. AIR OBSERVATIONS
CREATE TABLE IF NOT EXISTS air_observations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    station_id VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326),
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    pm25 DOUBLE PRECISION,
    pm10 DOUBLE PRECISION,
    no2 DOUBLE PRECISION,
    so2 DOUBLE PRECISION,
    co DOUBLE PRECISION,
    o3 DOUBLE PRECISION,
    aqi DOUBLE PRECISION,
    source VARCHAR(50) DEFAULT 'CPCB',
    data_quality VARCHAR(50) DEFAULT 'VALID',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. WEATHER OBSERVATIONS
CREATE TABLE IF NOT EXISTS weather_observations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326),
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    wind_direction DOUBLE PRECISION,
    rainfall DOUBLE PRECISION,
    pressure DOUBLE PRECISION,
    source VARCHAR(50) DEFAULT 'IMD',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. FIRE EVENTS (NASA FIRMS)
CREATE TABLE IF NOT EXISTS fire_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326),
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confidence DOUBLE PRECISION,
    frp DOUBLE PRECISION,
    satellite VARCHAR(50),
    source VARCHAR(50) DEFAULT 'NASA_FIRMS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. SATELLITE OBSERVATIONS (Sentinel-5P)
CREATE TABLE IF NOT EXISTS satellite_observations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    h3_index VARCHAR(30) NOT NULL,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    no2_value DOUBLE PRECISION,
    so2_value DOUBLE PRECISION,
    aerosol_indicator DOUBLE PRECISION,
    source_product VARCHAR(100) DEFAULT 'Sentinel-5P_O3',
    quality_flag VARCHAR(50) DEFAULT 'QA_PASS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. GRID CELLS (Uber H3 Index)
CREATE TABLE IF NOT EXISTS grid_cells (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    h3_index VARCHAR(30) UNIQUE NOT NULL,
    resolution INTEGER NOT NULL DEFAULT 8,
    center_latitude DOUBLE PRECISION NOT NULL,
    center_longitude DOUBLE PRECISION NOT NULL,
    boundary GEOGRAPHY(Polygon, 4326),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 8. GRID FEATURES
CREATE TABLE IF NOT EXISTS grid_features (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    feature_time TIMESTAMP WITH TIME ZONE NOT NULL,
    pm25_current DOUBLE PRECISION,
    pm25_lag_1h DOUBLE PRECISION,
    pm25_lag_3h DOUBLE PRECISION,
    pm25_trend DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    wind_direction DOUBLE PRECISION,
    rainfall DOUBLE PRECISION,
    fire_count INTEGER DEFAULT 0,
    nearest_fire_distance DOUBLE PRECISION,
    fire_confidence_avg DOUBLE PRECISION,
    satellite_no2 DOUBLE PRECISION,
    satellite_so2 DOUBLE PRECISION,
    citizen_report_count INTEGER DEFAULT 0,
    nearest_station_distance DOUBLE PRECISION,
    data_completeness DOUBLE PRECISION DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 9. HOTSPOT PREDICTIONS
CREATE TABLE IF NOT EXISTS hotspot_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    predicted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    explanation_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 10. FORECASTS
CREATE TABLE IF NOT EXISTS forecasts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    target_time TIMESTAMP WITH TIME ZONE NOT NULL,
    horizon_hours INTEGER NOT NULL,
    predicted_pm25 DOUBLE PRECISION NOT NULL,
    lower_bound DOUBLE PRECISION,
    upper_bound DOUBLE PRECISION,
    confidence DOUBLE PRECISION NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 11. USERS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'CITIZEN',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 12. CITIZEN REPORTS
CREATE TABLE IF NOT EXISTS citizen_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326),
    h3_index VARCHAR(30),
    category VARCHAR(50) NOT NULL,
    description TEXT,
    image_url TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(30) DEFAULT 'PENDING',
    verification_status VARCHAR(30) DEFAULT 'UNVERIFIED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 13. GEMINI ANALYSES
CREATE TABLE IF NOT EXISTS gemini_analyses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    citizen_report_id UUID REFERENCES citizen_reports(id) ON DELETE CASCADE,
    model_name VARCHAR(50) NOT NULL,
    detected_category VARCHAR(50),
    confidence DOUBLE PRECISION,
    narrative_summary TEXT,
    verification_required BOOLEAN DEFAULT TRUE,
    raw_response JSONB,
    analyzed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 14. POLLUTION EVENTS
CREATE TABLE IF NOT EXISTS pollution_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    event_code VARCHAR(50) UNIQUE NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) DEFAULT 'OPEN',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 15. EVENT EVIDENCE
CREATE TABLE IF NOT EXISTS event_evidence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES pollution_events(id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL,
    evidence_key VARCHAR(100) NOT NULL,
    evidence_value TEXT NOT NULL,
    weight DOUBLE PRECISION DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 16. ALERTS
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID REFERENCES pollution_events(id) ON DELETE SET NULL,
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    grid_cell_id UUID REFERENCES grid_cells(id) ON DELETE CASCADE,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    recommended_action TEXT,
    generated_by VARCHAR(50) DEFAULT 'MODEL_RULES',
    status VARCHAR(30) DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID REFERENCES users(id)
);

-- 17. AUTHORITY ACTIONS
CREATE TABLE IF NOT EXISTS authority_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id UUID REFERENCES alerts(id) ON DELETE CASCADE,
    action_type VARCHAR(100) NOT NULL,
    action_details TEXT NOT NULL,
    performed_by VARCHAR(150) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 18. INSPECTIONS
CREATE TABLE IF NOT EXISTS inspections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id UUID REFERENCES alerts(id) ON DELETE CASCADE,
    assigned_team VARCHAR(150) NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    findings TEXT,
    status VARCHAR(30) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 19. FEDERATED NODES
CREATE TABLE IF NOT EXISTS federated_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    node_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) DEFAULT 'ONLINE',
    model_version VARCHAR(50) NOT NULL,
    last_update_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 20. MODEL UPDATES
CREATE TABLE IF NOT EXISTS model_updates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_id UUID REFERENCES federated_nodes(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    sample_count INTEGER NOT NULL,
    metrics JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
