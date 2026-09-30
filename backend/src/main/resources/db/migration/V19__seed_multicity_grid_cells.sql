-- V19: Seed Real Multi-City H3 Grid Cells for Mumbai and Delhi
-- Ensures multi-city baseline hotspot detection and spatial coverage endpoints are fully operational

INSERT INTO grid_cells (id, city_id, h3_index, resolution, center_latitude, center_longitude, active)
VALUES
    ('6f000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440002', '88608b56b3fffff', 8, 19.08228, 72.88763, true),
    ('6f000000-0000-0000-0000-000000000004', '550e8400-e29b-41d4-a716-446655440003', '883da11505fffff', 8, 28.56410, 77.18725, true)
ON CONFLICT (h3_index) DO NOTHING;

-- Synchronize legacy model_version in hotspot_predictions if any legacy f3_classifier_v1 exists
UPDATE hotspot_predictions
SET model_version = 'hotspot_classifier_v1'
WHERE model_version = 'f3_classifier_v1';
