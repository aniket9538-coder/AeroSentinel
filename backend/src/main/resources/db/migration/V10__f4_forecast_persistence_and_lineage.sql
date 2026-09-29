-- V10: F4 Forecast Persistence, Lineage, and Integrity Constraints
-- Reconciles forecasts table with authoritative F4-P3 model inference contract

-- 1. Add required lineage, spatial, and contract columns
ALTER TABLE forecasts
    ADD COLUMN IF NOT EXISTS parent_prediction_id UUID REFERENCES hotspot_predictions(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS city_id UUID REFERENCES cities(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30),
    ADD COLUMN IF NOT EXISTS feature_snapshot_id UUID REFERENCES feature_snapshots(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS forecast_confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS unit VARCHAR(20) NOT NULL DEFAULT 'ug/m3',
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS';

-- 2. Make legacy confidence nullable (forecast confidence is strictly null)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'forecasts' AND column_name = 'confidence'
    ) THEN
        ALTER TABLE forecasts ALTER COLUMN confidence DROP NOT NULL;
    END IF;
END $$;

-- 3. Make grid_cell_id nullable since spatial indexing is primarily H3 resolution 8
ALTER TABLE forecasts ALTER COLUMN grid_cell_id DROP NOT NULL;

-- 4. Enforce data integrity constraints:
-- Horizon hours strictly in {1, 3, 6}
ALTER TABLE forecasts DROP CONSTRAINT IF EXISTS chk_forecast_horizon;
ALTER TABLE forecasts ADD CONSTRAINT chk_forecast_horizon CHECK (horizon_hours IN (1, 3, 6));

-- Physical and mathematical interval bounds: lower_bound >= 0 AND lower_bound <= predicted_pm25 AND predicted_pm25 <= upper_bound
ALTER TABLE forecasts DROP CONSTRAINT IF EXISTS chk_forecast_bounds;
ALTER TABLE forecasts ADD CONSTRAINT chk_forecast_bounds CHECK (
    lower_bound >= 0.0 AND lower_bound <= predicted_pm25 AND predicted_pm25 <= upper_bound
);

-- 5. Deterministic uniqueness / idempotency: exactly one forecast per parent prediction and horizon
ALTER TABLE forecasts DROP CONSTRAINT IF EXISTS uq_forecast_parent_horizon;
ALTER TABLE forecasts ADD CONSTRAINT uq_forecast_parent_horizon UNIQUE (parent_prediction_id, horizon_hours);

-- 6. Indexes for fast retrieval by lineage, spatial cell, and timeline
CREATE INDEX IF NOT EXISTS idx_forecasts_parent_pred ON forecasts(parent_prediction_id);
CREATE INDEX IF NOT EXISTS idx_forecasts_city_gen ON forecasts(city_id, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_forecasts_h3_gen ON forecasts(h3_index, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_forecasts_target ON forecasts(target_time ASC);
CREATE INDEX IF NOT EXISTS idx_forecasts_snapshot ON forecasts(feature_snapshot_id);
