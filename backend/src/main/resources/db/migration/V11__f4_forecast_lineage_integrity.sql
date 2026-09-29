-- V11: F4 Forecast Lineage Integrity and Non-Null Feature Snapshot Constraint
-- Enforces that all successful forecasts strictly preserve lineage to parent prediction,
-- city, H3 spatial cell, and feature snapshot while preserving legacy compatibility.

ALTER TABLE forecasts DROP CONSTRAINT IF EXISTS chk_forecast_success_lineage;

ALTER TABLE forecasts ADD CONSTRAINT chk_forecast_success_lineage CHECK (
    status <> 'SUCCESS'
    OR (
        parent_prediction_id IS NOT NULL
        AND city_id IS NOT NULL
        AND h3_index IS NOT NULL
        AND feature_snapshot_id IS NOT NULL
    )
);
