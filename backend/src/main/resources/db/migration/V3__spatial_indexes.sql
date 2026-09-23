-- AeroSentinel Spatial & Performance Indexes (V3)

-- PostGIS GiST Spatial Indexes
CREATE INDEX IF NOT EXISTS idx_stations_location ON monitoring_stations USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_air_obs_location ON air_observations USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_weather_obs_location ON weather_observations USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_fire_events_location ON fire_events USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_grid_cells_boundary ON grid_cells USING GIST(boundary);
CREATE INDEX IF NOT EXISTS idx_citizen_reports_location ON citizen_reports USING GIST(location);

-- Temporal & Relational Query Indexes
CREATE INDEX IF NOT EXISTS idx_air_obs_city_time ON air_observations(city_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_weather_obs_city_time ON weather_observations(city_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_fire_events_city_time ON fire_events(city_id, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_satellite_obs_h3_time ON satellite_observations(h3_index, observed_at DESC);

CREATE INDEX IF NOT EXISTS idx_grid_cells_h3 ON grid_cells(h3_index);
CREATE INDEX IF NOT EXISTS idx_grid_features_cell_time ON grid_features(grid_cell_id, feature_time DESC);
CREATE INDEX IF NOT EXISTS idx_hotspots_cell_time ON hotspot_predictions(grid_cell_id, predicted_at DESC);
CREATE INDEX IF NOT EXISTS idx_forecasts_cell_target ON forecasts(grid_cell_id, target_time ASC);

CREATE INDEX IF NOT EXISTS idx_alerts_status_created ON alerts(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_citizen_reports_status ON citizen_reports(status, submitted_at DESC);
