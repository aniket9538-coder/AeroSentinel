-- V15: F5-P5 Alert Candidate Lineage, Triage Integration, and Idempotency
-- Extends the alerts table to support evidence-backed alert candidate workflow and guarantees idempotency.

-- 1. Add spatial, prediction, and evidence lineage to alerts
ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30),
    ADD COLUMN IF NOT EXISTS prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS event_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS evidence_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS triage_state VARCHAR(30) DEFAULT 'ALERT_CANDIDATE',
    ADD COLUMN IF NOT EXISTS risk_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS forecast_summary TEXT,
    ADD COLUMN IF NOT EXISTS consistency VARCHAR(30),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolved_by UUID REFERENCES users(id);

-- 2. Indexing for fast queue lookups
CREATE INDEX IF NOT EXISTS idx_alerts_h3 ON alerts(h3_index);
CREATE INDEX IF NOT EXISTS idx_alerts_prediction ON alerts(prediction_id);
CREATE INDEX IF NOT EXISTS idx_alerts_event_code ON alerts(event_code);
CREATE INDEX IF NOT EXISTS idx_alerts_triage ON alerts(triage_state);
CREATE INDEX IF NOT EXISTS idx_alerts_status_created ON alerts(status, created_at DESC);

-- 3. Database-level idempotency protection: At most one alert per pollution event
CREATE UNIQUE INDEX IF NOT EXISTS idx_alerts_event_id_unique 
    ON alerts(event_id) WHERE event_id IS NOT NULL;
