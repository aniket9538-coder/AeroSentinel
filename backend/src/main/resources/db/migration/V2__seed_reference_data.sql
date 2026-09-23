-- AeroSentinel Reference Seed Data (V2)

-- Seed Cities (Pune, Mumbai, Delhi)
INSERT INTO cities (id, name, state, country, timezone, latitude, longitude, active)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'Pune', 'Maharashtra', 'India', 'Asia/Kolkata', 18.5204, 73.8567, true),
    ('550e8400-e29b-41d4-a716-446655440002', 'Mumbai', 'Maharashtra', 'India', 'Asia/Kolkata', 19.0760, 72.8777, true),
    ('550e8400-e29b-41d4-a716-446655440003', 'Delhi', 'Delhi NCR', 'India', 'Asia/Kolkata', 28.6139, 77.2090, true)
ON CONFLICT (id) DO NOTHING;

-- Seed CAAQMS Stations for Pune
INSERT INTO monitoring_stations (id, city_id, station_code, name, agency, latitude, longitude, location, status)
VALUES
    ('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'PUN-001', 'Shivajinagar CAAQMS', 'CPCB', 18.5314, 73.8446, ST_SetSRID(ST_MakePoint(73.8446, 18.5314), 4326), 'ACTIVE'),
    ('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'PUN-002', 'Katraj Air Station', 'MPCB', 18.4575, 73.8677, ST_SetSRID(ST_MakePoint(73.8677, 18.4575), 4326), 'ACTIVE'),
    ('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'PUN-003', 'Hadapsar Industrial Zone', 'MPCB', 18.5089, 73.9260, ST_SetSRID(ST_MakePoint(73.9260, 18.5089), 4326), 'ACTIVE')
ON CONFLICT (station_code) DO NOTHING;

-- Seed Federated Nodes
INSERT INTO federated_nodes (id, city_id, node_name, status, model_version)
VALUES
    ('770e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'Pune Municipal Node', 'ONLINE', 'xgb-pun-v1.2'),
    ('770e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'Mumbai Coastal Node', 'ONLINE', 'xgb-mum-v1.2'),
    ('770e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 'Delhi Regional Node', 'ONLINE', 'xgb-del-v1.2')
ON CONFLICT (id) DO NOTHING;
