-- V14: F5 Evidence Integrity, Pollution Event Lineage, and Event Evidence Hardening
-- Hardens relational integrity between hotspot predictions, pollution events, event evidence, and Gemini analyses.

-- 1. Add spatial and prediction lineage to pollution_events
ALTER TABLE pollution_events
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30),
    ADD COLUMN IF NOT EXISTS prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_pollution_events_h3 ON pollution_events(h3_index);
CREATE INDEX IF NOT EXISTS idx_pollution_events_prediction ON pollution_events(prediction_id);

-- 2. Add structured evidence attributes to event_evidence
ALTER TABLE event_evidence
    ADD COLUMN IF NOT EXISTS signal_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(100),
    ADD COLUMN IF NOT EXISTS relevance_tier VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_ref VARCHAR(100),
    ADD COLUMN IF NOT EXISTS confidence_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS observed_at TIMESTAMP WITH TIME ZONE;

-- 3. Deterministic duplicate evidence protection (unique per event and evidence_key/signal)
CREATE UNIQUE INDEX IF NOT EXISTS idx_event_evidence_dedup 
    ON event_evidence(event_id, evidence_key);

CREATE INDEX IF NOT EXISTS idx_event_evidence_signal ON event_evidence(signal_id);

-- 4. Add prediction_id to gemini_analyses to preserve both prediction and event linkage
ALTER TABLE gemini_analyses
    ADD COLUMN IF NOT EXISTS prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_gemini_analyses_prediction ON gemini_analyses(prediction_id);
