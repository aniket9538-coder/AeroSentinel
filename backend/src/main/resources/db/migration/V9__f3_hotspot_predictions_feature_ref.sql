-- V9: F3 Hotspot Predictions Schema Enhancement
-- Links hotspot predictions directly to feature_snapshots provenance, city_id, and h3_index

ALTER TABLE hotspot_predictions
ADD COLUMN IF NOT EXISTS feature_snapshot_id UUID REFERENCES feature_snapshots(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_hotspots_city_time ON hotspot_predictions(city_id, predicted_at DESC);
CREATE INDEX IF NOT EXISTS idx_hotspots_h3_time ON hotspot_predictions(h3_index, predicted_at DESC);
CREATE INDEX IF NOT EXISTS idx_hotspots_snapshot_id ON hotspot_predictions(feature_snapshot_id);
