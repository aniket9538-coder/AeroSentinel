-- V17: F6 Citizen Spatial Indexing and Gemini Analysis Relation Optimization

CREATE INDEX IF NOT EXISTS idx_citizen_reports_h3 ON citizen_reports(h3_index);
CREATE INDEX IF NOT EXISTS idx_gemini_analyses_citizen ON gemini_analyses(citizen_report_id);
