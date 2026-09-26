-- =====================================================================
-- AeroSentinel Migration V6: F2 Weather & H3 Spatial Layer Foundation
-- Feature: F2 - Real Weather + Centralized H3 Spatial Representation
-- =====================================================================

-- 1. AIR OBSERVATIONS: ADD H3 SPATIAL IDENTIFIER
-- Nullable column to preserve existing real F1 observations intact
ALTER TABLE air_observations 
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30);

-- Spatial and temporal lookup indexes for air observations
CREATE INDEX IF NOT EXISTS idx_air_obs_h3 
    ON air_observations (h3_index);

CREATE INDEX IF NOT EXISTS idx_air_obs_h3_time 
    ON air_observations (h3_index, observed_at DESC);

-- Coordinate integrity constraint for air observations
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_air_obs_coords'
    ) THEN
        ALTER TABLE air_observations
            ADD CONSTRAINT chk_air_obs_coords
            CHECK (latitude >= -90 AND latitude <= 90 AND longitude >= -180 AND longitude <= 180);
    END IF;
END $$;


-- 2. WEATHER OBSERVATIONS: SCHEMA HARDENING & H3 INTEGRATION
-- Add H3 spatial identifier matching repository convention
ALTER TABLE weather_observations 
    ADD COLUMN IF NOT EXISTS h3_index VARCHAR(30);

-- Ensure city_id is NOT NULL to guarantee relational integrity with cities
ALTER TABLE weather_observations 
    ALTER COLUMN city_id SET NOT NULL;

-- Spatial and temporal lookup indexes for weather observations
CREATE INDEX IF NOT EXISTS idx_weather_obs_h3 
    ON weather_observations (h3_index);

CREATE INDEX IF NOT EXISTS idx_weather_obs_h3_time 
    ON weather_observations (h3_index, observed_at DESC);

-- Duplicate protection: enforce at most one weather observation per city per timestamp
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_weather_obs_city_time'
    ) THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT uq_weather_obs_city_time
            UNIQUE (city_id, observed_at);
    END IF;
END $$;

-- Meteorological value range check constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_weather_obs_humidity') THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT chk_weather_obs_humidity
            CHECK (humidity IS NULL OR (humidity >= 0 AND humidity <= 100));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_weather_obs_wind_speed') THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT chk_weather_obs_wind_speed
            CHECK (wind_speed IS NULL OR wind_speed >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_weather_obs_wind_direction') THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT chk_weather_obs_wind_direction
            CHECK (wind_direction IS NULL OR (wind_direction >= 0 AND wind_direction <= 360));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_weather_obs_rainfall') THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT chk_weather_obs_rainfall
            CHECK (rainfall IS NULL OR rainfall >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_weather_obs_coords') THEN
        ALTER TABLE weather_observations
            ADD CONSTRAINT chk_weather_obs_coords
            CHECK (latitude >= -90 AND latitude <= 90 AND longitude >= -180 AND longitude <= 180);
    END IF;
END $$;

-- Spatial PostGIS synchronization trigger for weather observations
CREATE OR REPLACE FUNCTION sync_weather_obs_location()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.location IS NULL AND NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_weather_obs_location ON weather_observations;
CREATE TRIGGER trg_sync_weather_obs_location
BEFORE INSERT OR UPDATE ON weather_observations
FOR EACH ROW
EXECUTE FUNCTION sync_weather_obs_location();


-- 3. GRID CELLS: PERFORMANCE INDEXES & SPATIAL INTEGRITY
-- Index for rapid city-level cell retrieval
CREATE INDEX IF NOT EXISTS idx_grid_cells_city_id 
    ON grid_cells (city_id);

-- Center coordinate integrity constraint for grid cells
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_grid_cells_coords'
    ) THEN
        ALTER TABLE grid_cells
            ADD CONSTRAINT chk_grid_cells_coords
            CHECK (center_latitude >= -90 AND center_latitude <= 90 AND center_longitude >= -180 AND center_longitude <= 180);
    END IF;
END $$;
