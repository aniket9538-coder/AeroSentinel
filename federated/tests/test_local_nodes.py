"""
AeroSentinel - Unit Test Suite for Local City ML Nodes & Common Model Engine
File: federated/tests/test_local_nodes.py

Validates F9-P2 requirements:
1. Feature vector dimension & ordering (36 features matching f3-features-v1)
2. Pune local training
3. Mumbai local training
4. Delhi local training
5. Parameter update packaging
6. Global model parameter synchronization
7. Cross-city contract parity (FedAvg mathematical feasibility)
8. No raw data leakage in update payloads
"""

import os
import json
import numpy as np
import pandas as pd
import pytest

from federated.models.local_model import (
    LocalHotspotModel,
    FEATURE_SCHEMA_VERSION,
    FEATURE_COUNT,
    ORDERED_FEATURE_NAMES,
)
from federated.models.global_model import GlobalHotspotModel
from federated.clients.pune import PuneLocalDataLoader, PuneTrainer, PuneClient
from federated.clients.mumbai import MumbaiLocalDataLoader, MumbaiTrainer, MumbaiClient
from federated.clients.delhi import DelhiLocalDataLoader, DelhiTrainer, DelhiClient


class TestLocalNodesAndCommonModel:
    """Test suite for municipal ML nodes and common feature contracts."""

    def test_01_feature_vector_dimension_and_ordering(self):
        """Verify generated datasets for Pune, Mumbai, and Delhi have exactly 36 feature columns."""
        loaders = [
            ("Pune", PuneLocalDataLoader(random_seed=42)),
            ("Mumbai", MumbaiLocalDataLoader(random_seed=101)),
            ("Delhi", DelhiLocalDataLoader(random_seed=202)),
        ]

        for city_name, loader in loaders:
            df = loader.get_dataset(sample_count=200)
            feature_cols = [c for c in df.columns if c != "target_hotspot"]

            assert len(feature_cols) == FEATURE_COUNT, (
                f"{city_name} feature count {len(feature_cols)} != {FEATURE_COUNT}"
            )
            assert feature_cols == ORDERED_FEATURE_NAMES, (
                f"{city_name} feature sequence does not match ORDERED_FEATURE_NAMES exactly."
            )
            assert "target_hotspot" in df.columns, f"{city_name} missing target_hotspot"
            assert set(df["target_hotspot"].unique()).issubset({0, 1}), (
                f"{city_name} target_hotspot contains non-binary values"
            )

    def test_02_pune_local_training(self):
        """Train Pune local model and verify convergence, 36 weights, and valid metrics."""
        trainer = PuneTrainer(random_state=42)
        model, metrics, samples = trainer.train(sample_count=500)

        assert samples == 500
        assert model.is_fitted
        weights, intercept = model.get_weights()

        assert len(weights) == FEATURE_COUNT
        assert isinstance(intercept, float)
        assert not np.isnan(weights).any()
        assert not np.isnan(intercept)

        # Metric validation
        for key in ["mae", "rmse", "rocAuc", "brierScore", "accuracy"]:
            assert key in metrics, f"Missing metric {key} in Pune evaluation"
            assert isinstance(metrics[key], float)

        assert 0.5 <= metrics["rocAuc"] <= 1.0, f"Unreasonable ROC-AUC: {metrics['rocAuc']}"

    def test_03_mumbai_local_training(self):
        """Train Mumbai local model on coastal distribution and verify metrics."""
        trainer = MumbaiTrainer(random_state=101)
        model, metrics, samples = trainer.train(sample_count=400)

        assert samples == 400
        assert model.is_fitted
        weights, intercept = model.get_weights()

        assert len(weights) == FEATURE_COUNT
        assert not np.isnan(weights).any()
        assert 0.5 <= metrics["rocAuc"] <= 1.0

    def test_04_delhi_local_training(self):
        """Train Delhi local model on high particulate distribution and verify metrics."""
        trainer = DelhiTrainer(random_state=202)
        model, metrics, samples = trainer.train(sample_count=450)

        assert samples == 450
        assert model.is_fitted
        weights, intercept = model.get_weights()

        assert len(weights) == FEATURE_COUNT
        assert not np.isnan(weights).any()
        assert 0.5 <= metrics["rocAuc"] <= 1.0

    def test_05_parameter_update_packaging(self, tmp_path):
        """Execute package_model_update on Pune client and verify payload schema and serialization."""
        client = PuneClient(random_state=42)
        round_info = client.train_local_round(round_id="ROUND-001", base_version="global-v1", sample_count=300)

        artifact_dir = str(tmp_path / "updates")
        payload = client.package_model_update(
            round_id="ROUND-001",
            artifact_dir=artifact_dir,
        )

        assert payload["nodeId"] == "PUNE"
        assert payload["roundId"] == "ROUND-001"
        assert payload["baseModelVersion"] == "global-v1"
        assert payload["localModelVersion"] == "pune-r1"
        assert payload["sampleCount"] == 300
        assert len(payload["weights"]) == FEATURE_COUNT
        assert not np.isnan(payload["weights"]).any()
        assert not np.isnan(payload["intercept"])

        # Verify disk persistence
        artifact_path = payload["artifactReference"]
        assert os.path.exists(artifact_path)
        with open(artifact_path, "r", encoding="utf-8") as f:
            disk_payload = json.load(f)
        assert disk_payload["nodeId"] == "PUNE"
        assert len(disk_payload["weights"]) == FEATURE_COUNT

    def test_06_global_model_parameter_synchronization(self):
        """Verify GlobalHotspotModel parameter injection and inference consistency."""
        synthetic_weights = [round(float(np.sin(i)), 4) for i in range(FEATURE_COUNT)]
        synthetic_intercept = 0.25

        global_model = GlobalHotspotModel(
            version="global-v2",
            weights=synthetic_weights,
            intercept=synthetic_intercept,
        )

        assert global_model.version == "global-v2"
        assert len(global_model.weights) == FEATURE_COUNT

        # Verify inference on dummy test features
        loader = PuneLocalDataLoader(random_seed=42)
        test_df = loader.get_dataset(sample_count=10)
        probs = global_model.predict_proba(test_df)

        assert len(probs) == 10
        assert (probs >= 0.0).all() and (probs <= 1.0).all()

        # Synchronize into local model
        local_model = LocalHotspotModel()
        local_model.set_weights(global_model.weights, global_model.intercept)
        local_probs = local_model.predict_proba(test_df)

        assert len(local_probs) == 10
        assert (local_probs >= 0.0).all() and (local_probs <= 1.0).all()

    def test_07_cross_city_contract_parity(self, tmp_path):
        """Verify that updates from Pune, Mumbai, and Delhi have identical shapes and enable FedAvg."""
        artifact_dir = str(tmp_path / "updates")

        pune_client = PuneClient(random_state=42)
        mumbai_client = MumbaiClient(random_state=101)
        delhi_client = DelhiClient(random_state=202)

        pune_client.train_local_round("ROUND-001", "global-v1", sample_count=300)
        mumbai_client.train_local_round("ROUND-001", "global-v1", sample_count=200)
        delhi_client.train_local_round("ROUND-001", "global-v1", sample_count=250)

        pune_up = pune_client.package_model_update("ROUND-001", artifact_dir=artifact_dir)
        mumbai_up = mumbai_client.package_model_update("ROUND-001", artifact_dir=artifact_dir)
        delhi_up = delhi_client.package_model_update("ROUND-001", artifact_dir=artifact_dir)

        updates = [pune_up, mumbai_up, delhi_up]
        total_samples = sum(u["sampleCount"] for u in updates)
        assert total_samples == 750

        # Perform FedAvg simulation
        fed_weights = np.zeros(FEATURE_COUNT)
        fed_intercept = 0.0

        for u in updates:
            w_i = np.array(u["weights"])
            b_i = u["intercept"]
            s_i = u["sampleCount"]
            fed_weights += (s_i / total_samples) * w_i
            fed_intercept += (s_i / total_samples) * b_i

        assert len(fed_weights) == FEATURE_COUNT
        assert not np.isnan(fed_weights).any()
        assert not np.isnan(fed_intercept)

    def test_08_no_raw_data_leakage(self, tmp_path):
        """Assert that model update payloads contain zero raw data records, timestamps, or observations."""
        client = DelhiClient(random_state=202)
        client.train_local_round("ROUND-001", "global-v1", sample_count=200)

        artifact_dir = str(tmp_path / "updates")
        payload = client.package_model_update("ROUND-001", artifact_dir=artifact_dir)

        forbidden_keys = {
            "records",
            "telemetry",
            "raw_data",
            "observations",
            "rows",
            "coordinates",
            "raw_features",
        }

        assert not any(k in payload for k in forbidden_keys)
        assert "weights" in payload
        assert "metrics" in payload
        assert isinstance(payload["weights"], list)
        assert all(isinstance(w, (int, float)) for w in payload["weights"])

        # Check payload size (< 5 KB)
        json_str = json.dumps(payload)
        payload_size_bytes = len(json_str.encode("utf-8"))
        assert payload_size_bytes < 5120, f"Payload size {payload_size_bytes} exceeds 5KB threshold"
