-- V18: F9 Federated City Network Schema
-- Centralized coordination, node updates, rounds, and model registry

-- Clean up any legacy stub tables from V1
DROP TABLE IF EXISTS model_updates CASCADE;
DROP TABLE IF EXISTS federated_node_updates CASCADE;
DROP TABLE IF EXISTS federated_nodes CASCADE;

-- 1. federated_nodes
CREATE TABLE federated_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_id VARCHAR(50) NOT NULL UNIQUE,
    city_id UUID,
    node_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ONLINE',
    model_version VARCHAR(50) NOT NULL DEFAULT 'global-v1',
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    endpoint_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_federated_nodes_status ON federated_nodes(status);
CREATE INDEX IF NOT EXISTS idx_federated_nodes_city_id ON federated_nodes(city_id);

-- 2. federated_rounds
CREATE TABLE IF NOT EXISTS federated_rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id VARCHAR(50) NOT NULL UNIQUE,
    base_model_version VARCHAR(50) NOT NULL,
    target_model_version VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    participating_nodes JSONB NOT NULL DEFAULT '[]',
    min_quorum INT NOT NULL DEFAULT 2,
    total_samples INT DEFAULT 0,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_federated_rounds_status ON federated_rounds(status);
CREATE INDEX IF NOT EXISTS idx_federated_rounds_created_at ON federated_rounds(created_at DESC);

-- 3. federated_node_updates
CREATE TABLE IF NOT EXISTS federated_node_updates (
    update_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id VARCHAR(50) NOT NULL REFERENCES federated_rounds(round_id) ON DELETE CASCADE,
    city_name VARCHAR(50) NOT NULL,
    local_model_version VARCHAR(50) NOT NULL,
    base_model_version VARCHAR(50) NOT NULL,
    sample_count INT NOT NULL,
    weights_json JSONB,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    artifact_reference VARCHAR(255),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_round_city_update UNIQUE(round_id, city_name)
);

CREATE INDEX IF NOT EXISTS idx_federated_node_updates_round ON federated_node_updates(round_id);
CREATE INDEX IF NOT EXISTS idx_federated_node_updates_city ON federated_node_updates(city_name);

-- 4. model_updates (contract parity)
CREATE TABLE IF NOT EXISTS model_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id VARCHAR(50) NOT NULL REFERENCES federated_rounds(round_id) ON DELETE CASCADE,
    node_id VARCHAR(50) NOT NULL REFERENCES federated_nodes(node_id) ON DELETE CASCADE,
    base_model_version VARCHAR(50) NOT NULL,
    local_model_version VARCHAR(50) NOT NULL,
    sample_count INT NOT NULL,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    weights_json JSONB,
    artifact_reference VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_round_node_update UNIQUE(round_id, node_id)
);

CREATE INDEX IF NOT EXISTS idx_model_updates_round ON model_updates(round_id);
CREATE INDEX IF NOT EXISTS idx_model_updates_node ON model_updates(node_id);
CREATE INDEX IF NOT EXISTS idx_model_updates_status ON model_updates(status);

-- 5. federated_global_models
CREATE TABLE IF NOT EXISTS federated_global_models (
    version VARCHAR(50) PRIMARY KEY,
    base_model_version VARCHAR(50),
    round_id VARCHAR(50) REFERENCES federated_rounds(round_id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    feature_schema_version VARCHAR(50) NOT NULL DEFAULT 'f3-features-v1',
    algorithm VARCHAR(100) NOT NULL DEFAULT 'FED_AVG_RIDGE',
    total_samples INT DEFAULT 0,
    participating_nodes JSONB NOT NULL DEFAULT '[]',
    weights_json JSONB,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    artifact_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_federated_global_models_active ON federated_global_models(is_active);

-- Seed Initial Municipal Nodes
INSERT INTO federated_nodes (node_id, node_name, city_id, status, model_version, last_seen_at)
VALUES
  ('PUNE', 'Pune Municipal Environmental Node', '550e8400-e29b-41d4-a716-446655440001', 'ONLINE', 'global-v1', CURRENT_TIMESTAMP),
  ('MUMBAI', 'Mumbai BMC Environmental Node', '550e8400-e29b-41d4-a716-446655440002', 'ONLINE', 'global-v1', CURRENT_TIMESTAMP),
  ('DELHI', 'Delhi DPCC Network Node', '550e8400-e29b-41d4-a716-446655440003', 'ONLINE', 'global-v1', CURRENT_TIMESTAMP)
ON CONFLICT (node_id) DO NOTHING;

-- Seed Initial Global Consensus Model (global-v1)
INSERT INTO federated_global_models (
    version, base_model_version, round_id, is_active,
    feature_schema_version, algorithm, total_samples,
    participating_nodes, weights_json, mae, rmse, roc_auc, brier_score,
    artifact_path, created_at
) VALUES (
    'global-v1', NULL, NULL, TRUE,
    'f3-features-v1', 'FED_AVG_RIDGE', 0,
    '["PUNE", "MUMBAI", "DELHI"]', '[]', 0.0, 0.0, 0.5, 0.25,
    'storage/models/global/global-v1.joblib', CURRENT_TIMESTAMP
) ON CONFLICT (version) DO NOTHING;
