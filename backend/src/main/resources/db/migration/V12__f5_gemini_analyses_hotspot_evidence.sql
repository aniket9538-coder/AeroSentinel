-- V12: F5 Gemini Analysis Hotspot Evidence and Lineage Extension
-- Reconciles gemini_analyses table with F5 Evidence Orchestration contract

ALTER TABLE gemini_analyses
    ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES pollution_events(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30),
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS event_summary_public TEXT,
    ADD COLUMN IF NOT EXISTS event_summary_analyst TEXT,
    ADD COLUMN IF NOT EXISTS detected_condition VARCHAR(100),
    ADD COLUMN IF NOT EXISTS forecast_trajectory TEXT,
    ADD COLUMN IF NOT EXISTS uncertainty_statement TEXT,
    ADD COLUMN IF NOT EXISTS is_grounded BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Ensure legacy model_name constraint does not block hotspot analysis records
ALTER TABLE gemini_analyses ALTER COLUMN model_name DROP NOT NULL;

-- Spatial and temporal indexes for high-throughput lookup
CREATE INDEX IF NOT EXISTS idx_gemini_analyses_h3 ON gemini_analyses(h3_index);
CREATE INDEX IF NOT EXISTS idx_gemini_analyses_event ON gemini_analyses(event_id);
CREATE INDEX IF NOT EXISTS idx_gemini_analyses_created ON gemini_analyses(created_at DESC);
