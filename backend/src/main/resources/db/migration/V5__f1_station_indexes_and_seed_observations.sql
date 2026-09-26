-- =====================================================================
-- AeroSentinel Migration V5: F1 Station Indexes & Deterministic Seed Observations
-- Feature: F1 - City + Air Quality Foundation
-- =====================================================================

-- 1. ADD PERFORMANCE & HISTORICAL QUERY INDEXES
CREATE INDEX IF NOT EXISTS idx_air_obs_station_time 
    ON air_observations (station_id, observed_at DESC);

CREATE INDEX IF NOT EXISTS idx_air_obs_observed_at 
    ON air_observations (observed_at DESC);

-- 2. ENSURE QUALITY COLUMN AND DATA INTEGRITY
-- Add quality column if not present (aligns with F1 logical schema requirements)
ALTER TABLE air_observations 
    ADD COLUMN IF NOT EXISTS quality VARCHAR(50) DEFAULT 'VALID';

-- Populate existing/legacy records
UPDATE air_observations 
    SET quality = data_quality 
    WHERE quality IS NULL AND data_quality IS NOT NULL;

-- Trigger to keep quality and data_quality synchronized for backward compatibility
CREATE OR REPLACE FUNCTION sync_air_obs_quality()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quality IS NULL AND NEW.data_quality IS NOT NULL THEN
        NEW.quality := NEW.data_quality;
    ELSIF NEW.data_quality IS NULL AND NEW.quality IS NOT NULL THEN
        NEW.data_quality := NEW.quality;
    ELSIF NEW.quality IS NULL AND NEW.data_quality IS NULL THEN
        NEW.quality := 'VALID';
        NEW.data_quality := 'VALID';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_air_obs_quality ON air_observations;
CREATE TRIGGER trg_sync_air_obs_quality
BEFORE INSERT OR UPDATE ON air_observations
FOR EACH ROW
EXECUTE FUNCTION sync_air_obs_quality();

-- 3. ENFORCE DATA INTEGRITY CONSTRAINTS
-- PM2.5 cannot be negative
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_air_obs_pm25'
    ) THEN
        ALTER TABLE air_observations
            ADD CONSTRAINT chk_air_obs_pm25
            CHECK (pm25 IS NULL OR pm25 >= 0);
    END IF;
END $$;

-- Relational integrity: station_id must reference a valid monitoring station
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_air_obs_station'
    ) THEN
        ALTER TABLE air_observations
            ADD CONSTRAINT fk_air_obs_station
            FOREIGN KEY (station_id) REFERENCES monitoring_stations(station_code)
            ON DELETE CASCADE;
    END IF;
END $$;

-- Source cannot be null
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_air_obs_source_not_null'
    ) THEN
        ALTER TABLE air_observations
            ADD CONSTRAINT chk_air_obs_source_not_null
            CHECK (source IS NOT NULL);
    END IF;
END $$;

-- Quality cannot be null
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_air_obs_quality_not_null'
    ) THEN
        ALTER TABLE air_observations
            ADD CONSTRAINT chk_air_obs_quality_not_null
            CHECK (quality IS NOT NULL);
    END IF;
END $$;

-- 4. SEED DETERMINISTIC DEVELOPMENT / DEMO OBSERVATIONS FOR PUNE STATIONS (24-Hour Trend)
-- City: Pune (550e8400-e29b-41d4-a716-446655440001)
-- Stations: PUN-001 (Shivajinagar), PUN-002 (Katraj), PUN-003 (Hadapsar)
-- Source: CPCB (PUN-001), MPCB (PUN-002, PUN-003)
-- Quality: VALID

-- Station PUN-001: Shivajinagar CAAQMS (18.5314, 73.8446) - 12 historical observations across 24h
INSERT INTO air_observations (
    id, city_id, station_id, latitude, longitude, location, 
    observed_at, pm25, pm10, no2, so2, co, o3, aqi, 
    source, data_quality, quality, created_at
) VALUES
    ('880e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 00:00:00+00', 72.0, 110.0, 34.0, 13.0, 0.8, 22.0, 145.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 02:00:00+00', 95.0, 146.0, 47.0, 17.0, 1.2, 29.0, 173.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 04:00:00+00', 118.0, 178.0, 56.0, 22.0, 1.5, 38.0, 195.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 06:00:00+00', 104.0, 162.0, 51.0, 20.0, 1.3, 35.0, 184.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 08:00:00+00', 82.0, 128.0, 38.0, 15.0, 1.0, 28.0, 158.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 10:00:00+00', 76.0, 118.0, 36.0, 14.0, 0.9, 25.0, 152.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 12:00:00+00', 92.0, 142.0, 45.0, 18.0, 1.1, 31.0, 170.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 14:00:00+00', 122.0, 184.0, 58.0, 24.0, 1.6, 41.0, 198.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440009', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 16:00:00+00', 112.0, 170.0, 54.0, 22.0, 1.5, 39.0, 191.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 18:00:00+00', 98.0, 150.0, 48.0, 19.0, 1.3, 33.0, 176.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 20:00:00+00', 86.0, 134.0, 42.0, 16.0, 1.1, 28.0, 163.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440012', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326),
     '2026-09-24 22:00:00+00', 78.0, 120.0, 37.0, 14.0, 0.9, 24.0, 155.0, 'CPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00')
ON CONFLICT (id) DO NOTHING;

-- Station PUN-002: Katraj Air Station (18.4575, 73.8677) - 12 historical observations across 24h
INSERT INTO air_observations (
    id, city_id, station_id, latitude, longitude, location, 
    observed_at, pm25, pm10, no2, so2, co, o3, aqi, 
    source, data_quality, quality, created_at
) VALUES
    ('880e8400-e29b-41d4-a716-446655440013', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 00:00:00+00', 58.0, 88.0, 25.0, 10.0, 0.6, 18.0, 116.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440014', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 02:00:00+00', 74.0, 116.0, 35.0, 14.0, 0.9, 25.0, 146.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440015', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 04:00:00+00', 92.0, 142.0, 44.0, 18.0, 1.2, 31.0, 168.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440016', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 06:00:00+00', 82.0, 128.0, 39.0, 16.0, 1.0, 28.0, 157.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440017', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 08:00:00+00', 64.0, 100.0, 29.0, 12.0, 0.8, 22.0, 131.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440018', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 10:00:00+00', 60.0, 94.0, 27.0, 11.0, 0.7, 20.0, 122.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440019', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 12:00:00+00', 72.0, 112.0, 33.0, 13.0, 0.9, 24.0, 143.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440020', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 14:00:00+00', 94.0, 144.0, 45.0, 19.0, 1.2, 32.0, 170.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440021', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 16:00:00+00', 88.0, 136.0, 43.0, 18.0, 1.1, 30.0, 164.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440022', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 18:00:00+00', 78.0, 122.0, 37.0, 15.0, 1.0, 26.0, 153.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440023', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 20:00:00+00', 68.0, 106.0, 31.0, 12.0, 0.8, 23.0, 137.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440024', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326),
     '2026-09-24 22:00:00+00', 62.0, 96.0, 28.0, 11.0, 0.7, 20.0, 126.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00')
ON CONFLICT (id) DO NOTHING;

-- Station PUN-003: Hadapsar Industrial Zone (18.5089, 73.9260) - 12 historical observations across 24h
INSERT INTO air_observations (
    id, city_id, station_id, latitude, longitude, location, 
    observed_at, pm25, pm10, no2, so2, co, o3, aqi, 
    source, data_quality, quality, created_at
) VALUES
    ('880e8400-e29b-41d4-a716-446655440025', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 00:00:00+00', 76.0, 122.0, 40.0, 16.0, 1.0, 24.0, 151.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440026', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 02:00:00+00', 105.0, 165.0, 56.0, 24.0, 1.5, 34.0, 185.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440027', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 04:00:00+00', 128.0, 202.0, 70.0, 31.0, 1.8, 43.0, 206.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440028', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 06:00:00+00', 114.0, 180.0, 62.0, 27.0, 1.6, 39.0, 193.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440029', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 08:00:00+00', 90.0, 144.0, 48.0, 21.0, 1.3, 31.0, 168.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440030', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 10:00:00+00', 84.0, 134.0, 44.0, 19.0, 1.2, 29.0, 160.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440031', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 12:00:00+00', 102.0, 160.0, 54.0, 23.0, 1.4, 34.0, 182.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440032', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 14:00:00+00', 132.0, 208.0, 72.0, 32.0, 1.9, 45.0, 210.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440033', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 16:00:00+00', 124.0, 194.0, 68.0, 30.0, 1.8, 44.0, 202.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440034', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 18:00:00+00', 110.0, 174.0, 60.0, 26.0, 1.6, 38.0, 190.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440035', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 20:00:00+00', 96.0, 152.0, 52.0, 22.0, 1.4, 33.0, 175.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00'),
    ('880e8400-e29b-41d4-a716-446655440036', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326),
     '2026-09-24 22:00:00+00', 86.0, 138.0, 46.0, 20.0, 1.2, 30.0, 163.0, 'MPCB', 'VALID', 'VALID', '2026-09-24 23:00:00+00')
ON CONFLICT (id) DO NOTHING;
