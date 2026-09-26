-- V8: F3 Feature Snapshots Persistence Layer
-- Supports reproducible, auditable 36-feature vectors for ML training & inference contracts

CREATE TABLE IF NOT EXISTS feature_snapshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    h3_index VARCHAR(30) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    feature_schema_version VARCHAR(50) NOT NULL DEFAULT 'f3-features-v1',
    features JSONB NOT NULL,
    quality_status VARCHAR(30) NOT NULL DEFAULT 'VALID',
    missing_features TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT uq_feature_snapshot_cell_time_version UNIQUE (h3_index, observed_at, feature_schema_version)
);

CREATE INDEX IF NOT EXISTS idx_feature_snapshots_cell_time ON feature_snapshots(h3_index, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_feature_snapshots_city_time ON feature_snapshots(city_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_feature_snapshots_quality ON feature_snapshots(quality_status);
