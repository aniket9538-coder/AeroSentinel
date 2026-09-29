-- ============================================================
-- AeroSentinel — Feature 5 Phase 6 (F5-P6)
-- Field Teams, Alert Assignments, and Field Verification Schema
-- ============================================================

-- 1. FIELD TEAMS TABLE
CREATE TABLE IF NOT EXISTS field_teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_code VARCHAR(50) NOT NULL UNIQUE,
    team_name VARCHAR(150) NOT NULL,
    city_id UUID REFERENCES cities(id) ON DELETE SET NULL,
    status VARCHAR(30) DEFAULT 'AVAILABLE',
    contact_number VARCHAR(50),
    leader_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_field_teams_city ON field_teams(city_id);
CREATE INDEX IF NOT EXISTS idx_field_teams_status ON field_teams(status);

-- 2. EVOLVE INSPECTIONS TABLE (Alert Assignments & Field Inspection)
ALTER TABLE inspections
    ADD COLUMN IF NOT EXISTS team_id UUID REFERENCES field_teams(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES pollution_events(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30),
    ADD COLUMN IF NOT EXISTS prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

ALTER TABLE inspections ALTER COLUMN assigned_team DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inspections_alert ON inspections(alert_id);
CREATE INDEX IF NOT EXISTS idx_inspections_team ON inspections(team_id);
CREATE INDEX IF NOT EXISTS idx_inspections_event ON inspections(event_id);
CREATE INDEX IF NOT EXISTS idx_inspections_h3 ON inspections(h3_index);
CREATE INDEX IF NOT EXISTS idx_inspections_status ON inspections(status);

-- Unique partial index to prevent duplicate active assignments for the same alert
CREATE UNIQUE INDEX IF NOT EXISTS idx_inspections_active_alert_unique
    ON inspections(alert_id)
    WHERE status IN ('SCHEDULED', 'ASSIGNED', 'IN_PROGRESS');

-- 3. FIELD VERIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS field_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE CASCADE,
    alert_id UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    event_id UUID REFERENCES pollution_events(id) ON DELETE CASCADE,
    h3_index VARCHAR(30) NOT NULL,
    prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE SET NULL,
    verification_status VARCHAR(50) NOT NULL,
    verification_result VARCHAR(50),
    observed_conditions TEXT NOT NULL,
    inspector_notes TEXT,
    evidence_references TEXT,
    verified_by VARCHAR(150),
    inspected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verifications_inspection ON field_verifications(inspection_id);
CREATE INDEX IF NOT EXISTS idx_verifications_alert ON field_verifications(alert_id);
CREATE INDEX IF NOT EXISTS idx_verifications_h3 ON field_verifications(h3_index);
CREATE INDEX IF NOT EXISTS idx_verifications_status ON field_verifications(verification_status);

-- 4. SEED LEGITIMATE MUNICIPAL FIELD TEAMS
INSERT INTO field_teams (id, team_code, team_name, city_id, status, contact_number, leader_name)
VALUES
    ('770e8400-e29b-41d4-a716-446655440001', 'TEAM-PUN-01', 'Pune Municipal Rapid Response Team A', '550e8400-e29b-41d4-a716-446655440001', 'AVAILABLE', '+91-20-25501001', 'Inspector A. Deshmukh'),
    ('770e8400-e29b-41d4-a716-446655440002', 'TEAM-PUN-02', 'Pune Industrial Anti-Smog Squad', '550e8400-e29b-41d4-a716-446655440001', 'AVAILABLE', '+91-20-25501002', 'Officer R. Kulkarni'),
    ('770e8400-e29b-41d4-a716-446655440003', 'TEAM-MUM-01', 'Mumbai Coastal Monitoring Unit 1', '550e8400-e29b-41d4-a716-446655440002', 'AVAILABLE', '+91-22-22661001', 'Inspector S. Patil'),
    ('770e8400-e29b-41d4-a716-446655440004', 'TEAM-DEL-01', 'Delhi Air Enforcement Flying Squad 1', '550e8400-e29b-41d4-a716-446655440003', 'AVAILABLE', '+91-11-23371001', 'Officer V. Sharma')
ON CONFLICT (team_code) DO NOTHING;
