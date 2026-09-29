-- V13: Relax foreign key constraint on gemini_analyses.event_id to support standalone hotspot prediction lineage
ALTER TABLE gemini_analyses DROP CONSTRAINT IF EXISTS gemini_analyses_event_id_fkey;
CREATE INDEX IF NOT EXISTS idx_gemini_analyses_event_id ON gemini_analyses(event_id);
