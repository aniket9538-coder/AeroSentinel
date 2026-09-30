"""
AeroSentinel - Unit Test Suite for Federated Coordinator, Aggregator & Round Manager
File: federated/tests/test_coordinator.py

Validates F9-P3 requirements:
1. Complete 3-node federated round simulation (Pune + Mumbai + Delhi -> global-v2)
2. Quorum threshold enforcement (2 of 3 nodes succeeds)
3. Insufficient quorum failure (< 2 nodes blocks aggregation)
4. Stale base model update rejection
5. Duplicate node update rejection
6. Mismatched weight dimension rejection
7. Model registry version lineage & active model tracking
8. Mathematical FedAvg correctness verification
"""

import os
import json
import numpy as np
import pytest
from federated.models.local_model import FEATURE_COUNT
from federated.models.global_model import GlobalHotspotModel
from federated.coordinator import (
    FederatedAggregator,
    RoundManager,
    RoundState,
    ModelRegistry,
    FederatedCoordinator,
)
from federated.clients.pune import PuneClient
from federated.clients.mumbai import MumbaiClient
from federated.clients.delhi import DelhiClient


class TestCoordinatorAndAggregator:
    """Test suite for coordinator state machine, aggregator math, and model registry."""

    def test_01_complete_3_node_round_simulation(self, tmp_path):
        """Pune, Mumbai, Delhi train and submit updates -> aggregation succeeds -> global-v2 created and active."""
        storage_dir = str(tmp_path / "global_models")
        coordinator = FederatedCoordinator(storage_dir=storage_dir)

        assert coordinator.model_registry.active_version == "global-v1"

        # 1. Start Round 1
        round_info = coordinator.start_round(round_id="ROUND-001")
        assert round_info["roundId"] == "ROUND-001"
        assert round_info["baseModelVersion"] == "global-v1"
        assert round_info["status"] == "CREATED"

        # 2. Local Training on Pune, Mumbai, Delhi
        pune = PuneClient(random_state=42)
        mumbai = MumbaiClient(random_state=101)
        delhi = DelhiClient(random_state=202)

        pune.train_local_round("ROUND-001", "global-v1", sample_count=300)
        mumbai.train_local_round("ROUND-001", "global-v1", sample_count=200)
        delhi.train_local_round("ROUND-001", "global-v1", sample_count=250)

        updates_dir = str(tmp_path / "updates")
        up_pune = pune.package_model_update("ROUND-001", artifact_dir=updates_dir)
        up_mumbai = mumbai.package_model_update("ROUND-001", artifact_dir=updates_dir)
        up_delhi = delhi.package_model_update("ROUND-001", artifact_dir=updates_dir)

        # 3. Submit updates to coordinator
        coordinator.submit_node_update("ROUND-001", up_pune)
        coordinator.submit_node_update("ROUND-001", up_mumbai)
        coordinator.submit_node_update("ROUND-001", up_delhi)

        status = coordinator.get_round_status("ROUND-001")
        assert status["receivedUpdatesCount"] == 3
        assert status["status"] == "UPDATES_COLLECTING"

        # 4. Trigger aggregation
        agg_res = coordinator.trigger_aggregation("ROUND-001")
        assert agg_res["status"] == "COMPLETED"
        assert agg_res["globalModelVersion"] == "global-v2"
        assert agg_res["participatingNodes"] == 3
        assert agg_res["totalSamples"] == 750

        # 5. Verify active model updated
        active_model = coordinator.get_active_global_model()
        assert active_model.version == "global-v2"
        assert len(active_model.weights) == FEATURE_COUNT

    def test_02_quorum_threshold_two_of_three(self, tmp_path):
        """Round aggregates successfully with only 2 nodes (e.g. Pune + Delhi)."""
        storage_dir = str(tmp_path / "global_models")
        coordinator = FederatedCoordinator(storage_dir=storage_dir, min_quorum=2)

        coordinator.start_round(round_id="ROUND-001", base_version="global-v1")

        pune = PuneClient(random_state=42)
        delhi = DelhiClient(random_state=202)

        pune.train_local_round("ROUND-001", "global-v1", sample_count=300)
        delhi.train_local_round("ROUND-001", "global-v1", sample_count=200)

        up_pune = pune.package_model_update("ROUND-001", artifact_dir=str(tmp_path / "updates"))
        up_delhi = delhi.package_model_update("ROUND-001", artifact_dir=str(tmp_path / "updates"))

        coordinator.submit_node_update("ROUND-001", up_pune)
        coordinator.submit_node_update("ROUND-001", up_delhi)

        agg_res = coordinator.trigger_aggregation("ROUND-001")
        assert agg_res["status"] == "COMPLETED"
        assert agg_res["participatingNodes"] == 2
        assert agg_res["totalSamples"] == 500

    def test_03_insufficient_quorum_blocks_aggregation(self, tmp_path):
        """Aggregation blocked and round fails when fewer than minimum quorum received."""
        storage_dir = str(tmp_path / "global_models")
        coordinator = FederatedCoordinator(storage_dir=storage_dir, min_quorum=2)

        coordinator.start_round(round_id="ROUND-001", base_version="global-v1")

        pune = PuneClient(random_state=42)
        pune.train_local_round("ROUND-001", "global-v1", sample_count=300)
        up_pune = pune.package_model_update("ROUND-001", artifact_dir=str(tmp_path / "updates"))

        # Submit only 1 update (quorum is 2)
        coordinator.submit_node_update("ROUND-001", up_pune)

        with pytest.raises(ValueError, match="Insufficient quorum"):
            coordinator.trigger_aggregation("ROUND-001")

        status = coordinator.get_round_status("ROUND-001")
        assert status["status"] == "FAILED"
        assert "Insufficient quorum" in status["failureReason"]

    def test_04_stale_base_model_rejection(self, tmp_path):
        """Update referencing baseModelVersion != round.baseModelVersion is rejected."""
        storage_dir = str(tmp_path / "global_models")
        coordinator = FederatedCoordinator(storage_dir=storage_dir)

        coordinator.start_round(round_id="ROUND-002", base_version="global-v2")

        pune = PuneClient(random_state=42)
        # Train on stale global-v1
        pune.train_local_round("ROUND-002", "global-v1", sample_count=300)
        stale_up = pune.package_model_update("ROUND-002", artifact_dir=str(tmp_path / "updates"))

        with pytest.raises(ValueError, match="Stale update"):
            coordinator.submit_node_update("ROUND-002", stale_up)

    def test_05_duplicate_update_rejection(self, tmp_path):
        """Second update from same node in same round is rejected."""
        storage_dir = str(tmp_path / "global_models")
        coordinator = FederatedCoordinator(storage_dir=storage_dir)

        coordinator.start_round(round_id="ROUND-001", base_version="global-v1")

        pune = PuneClient(random_state=42)
        pune.train_local_round("ROUND-001", "global-v1", sample_count=300)
        up_pune = pune.package_model_update("ROUND-001", artifact_dir=str(tmp_path / "updates"))

        coordinator.submit_node_update("ROUND-001", up_pune)

        # Attempt to submit duplicate
        with pytest.raises(ValueError, match="Duplicate update"):
            coordinator.submit_node_update("ROUND-001", up_pune)

    def test_06_mismatched_weight_dimension_rejection(self, tmp_path):
        """Update with != 36 weights is rejected."""
        storage_dir = str(tmp_path / "global_models")
        coordinator = FederatedCoordinator(storage_dir=storage_dir)

        coordinator.start_round(round_id="ROUND-001", base_version="global-v1")

        invalid_payload = {
            "nodeId": "PUNE",
            "roundId": "ROUND-001",
            "baseModelVersion": "global-v1",
            "sampleCount": 100,
            "weights": [0.1, 0.2, 0.3],  # Length 3 instead of 36
            "intercept": 0.0,
            "metrics": {"mae": 0.1, "rmse": 0.2},
        }

        with pytest.raises(ValueError, match=f"weights length 3 != {FEATURE_COUNT}"):
            coordinator.submit_node_update("ROUND-001", invalid_payload)

    def test_07_model_registry_version_lineage(self, tmp_path):
        """Verify global-v1 -> global-v2 -> global-v3 lineage and active pointer updates."""
        storage_dir = str(tmp_path / "global_models")
        registry = ModelRegistry(storage_dir=storage_dir)

        assert registry.active_version == "global-v1"

        # Register global-v2
        m2 = GlobalHotspotModel(
            version="global-v2",
            weights=[0.1] * FEATURE_COUNT,
            intercept=0.05,
        )
        registry.register_model(m2, is_active=True)
        assert registry.active_version == "global-v2"

        # Register global-v3
        m3 = GlobalHotspotModel(
            version="global-v3",
            weights=[0.2] * FEATURE_COUNT,
            intercept=0.10,
        )
        registry.register_model(m3, is_active=True)
        assert registry.active_version == "global-v3"

        # List models and check active status
        catalog = registry.list_models()
        assert len(catalog) >= 3
        active_entry = next(c for c in catalog if c["version"] == "global-v3")
        assert active_entry["status"] == "ACTIVE"
        prev_entry = next(c for c in catalog if c["version"] == "global-v2")
        assert prev_entry["status"] == "SUPERSEDED"

    def test_08_mathematical_fedavg_correctness(self):
        """Verify aggregated weights match manual mathematical calculation."""
        aggregator = FederatedAggregator()

        # Update 1: 100 samples, all weights = 1.0, intercept = 0.5
        u1 = {
            "nodeId": "PUNE",
            "sampleCount": 100,
            "weights": [1.0] * FEATURE_COUNT,
            "intercept": 0.5,
            "metrics": {"mae": 0.20, "rmse": 0.30, "rocAuc": 0.80, "brierScore": 0.10},
        }

        # Update 2: 300 samples, all weights = 3.0, intercept = 1.5
        u2 = {
            "nodeId": "DELHI",
            "sampleCount": 300,
            "weights": [3.0] * FEATURE_COUNT,
            "intercept": 1.5,
            "metrics": {"mae": 0.40, "rmse": 0.50, "rocAuc": 0.90, "brierScore": 0.20},
        }

        # Total samples = 400
        # Expected weight = (100*1.0 + 300*3.0)/400 = (100 + 900)/400 = 1000/400 = 2.5
        # Expected intercept = (100*0.5 + 300*1.5)/400 = (50 + 450)/400 = 500/400 = 1.25
        # Expected MAE = (100*0.20 + 300*0.40)/400 = (20 + 120)/400 = 140/400 = 0.35
        # Expected ROC-AUC = (100*0.80 + 300*0.90)/400 = (80 + 270)/400 = 350/400 = 0.875
        # Expected RMSE = sqrt((100*0.09 + 300*0.25)/400) = sqrt((9 + 75)/400) = sqrt(84/400) = sqrt(0.21) ≈ 0.4583

        res = aggregator.aggregate([u1, u2])

        assert res["totalSamples"] == 400
        assert np.allclose(res["weights"], [2.5] * FEATURE_COUNT)
        assert np.isclose(res["intercept"], 1.25)
        assert np.isclose(res["metrics"]["mae"], 0.35)
        assert np.isclose(res["metrics"]["rocAuc"], 0.875)
        assert np.isclose(res["metrics"]["rmse"], np.sqrt(0.21), atol=1e-4)
