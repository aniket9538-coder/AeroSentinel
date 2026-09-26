-- =====================================================================
-- AeroSentinel Migration V7: Spatial Weather Uniqueness Constraint
-- Feature: F2 - Spatial Weather Integration & Centralized H3 Layer
-- =====================================================================

-- Drop the city-wide unique constraint so spatial weather observations
-- for multiple stations/H3 cells within the same city can coexist at the same timestamp
ALTER TABLE weather_observations 
    DROP CONSTRAINT IF EXISTS uq_weather_obs_city_time;

-- Add spatial uniqueness constraint: at most one weather observation per H3 cell per timestamp
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_weather_obs_spatial_time'
    ) THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT uq_weather_obs_spatial_time
            UNIQUE (h3_index, observed_at);
    END IF;
END $$;
