"""
AeroSentinel - Base Federated Client and Trainer
File: federated/clients/base_client.py

Provides common base classes (BaseCityTrainer, BaseFederatedClient) ensuring DRY code
and strict adherence to the 36-feature schema and model update payload contracts.
"""

from typing import Dict, List, Optional, Tuple, Any
import os
import json
import numpy as np
import pandas as pd
from datetime import datetime, timezone
from sklearn.model_selection import train_test_split

from federated.models.local_model import (
    LocalHotspotModel,
    FEATURE_SCHEMA_VERSION,
    FEATURE_COUNT,
    ORDERED_FEATURE_NAMES,
)


class BaseCityTrainer:
    """
    Base trainer for municipal nodes.
    Splits local data into train/val, fits LocalHotspotModel, and computes metrics.
    """

    def __init__(self, node_id: str, city_id: str, data_loader, random_state: int = 42):
        self.node_id = node_id
        self.city_id = city_id
        self.data_loader = data_loader
        self.random_state = random_state

    def load_data(self, sample_count: Optional[int] = None) -> Tuple[pd.DataFrame, pd.Series]:
        """Loads or generates isolated municipal dataset."""
        df = self.data_loader.get_dataset(sample_count=sample_count)
        X = df[ORDERED_FEATURE_NAMES]
        y = df["target_hotspot"]
        return X, y

    def train(
        self,
        base_weights: Optional[List[float]] = None,
        base_intercept: Optional[float] = None,
        sample_count: Optional[int] = None,
    ) -> Tuple[LocalHotspotModel, Dict[str, float], int]:
        """
        Executes local training and validation.
        Optionally warm-starts from global base weights.
        """
        X, y = self.load_data(sample_count=sample_count)
        total_samples = len(X)

        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=0.20, random_state=self.random_state, stratify=y
        )

        model = LocalHotspotModel(random_state=self.random_state)

        # If base weights provided, warm-start initialization
        if base_weights is not None and base_intercept is not None:
            model.set_weights(base_weights, base_intercept)

        # Fit model on local training split
        model.fit(X_train, y_train)

        # Evaluate on local validation split
        metrics = model.evaluate(X_val, y_val)
        return model, metrics, total_samples


class BaseFederatedClient:
    """
    Base client coordinator for municipal nodes.
    Receives global model, runs local training, and packages serialized model updates.
    """

    def __init__(
        self,
        node_id: str,
        city_id: str,
        trainer: BaseCityTrainer,
        network_client: Optional[Any] = None,
    ):
        self.node_id = node_id
        self.city_id = city_id
        self.trainer = trainer
        self.network_client = network_client
        self.current_model: Optional[LocalHotspotModel] = None
        self.base_model_version: str = "global-v1"
        self.base_weights: Optional[List[float]] = None
        self.base_intercept: Optional[float] = None
        self.latest_metrics: Dict[str, float] = {}
        self.latest_sample_count: int = 0

    def receive_global_model(
        self,
        base_weights: List[float],
        base_intercept: float,
        base_version: str,
    ) -> None:
        """
        Receives new consensus model parameters from coordinator.
        """
        if len(base_weights) != FEATURE_COUNT:
            raise ValueError(f"Invalid global weights length: {len(base_weights)} != {FEATURE_COUNT}")

        self.base_weights = [float(w) for w in base_weights]
        self.base_intercept = float(base_intercept)
        self.base_model_version = base_version

        if self.current_model is None:
            self.current_model = LocalHotspotModel()
        self.current_model.set_weights(self.base_weights, self.base_intercept)

    def train_local_round(
        self,
        round_id: str,
        base_version: str,
        sample_count: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        Executes local round training and returns evaluation metrics.
        """
        self.base_model_version = base_version
        model, metrics, samples = self.trainer.train(
            base_weights=self.base_weights,
            base_intercept=self.base_intercept,
            sample_count=sample_count,
        )
        self.current_model = model
        self.latest_metrics = metrics
        self.latest_sample_count = samples

        return {
            "nodeId": self.node_id,
            "roundId": round_id,
            "sampleCount": samples,
            "metrics": metrics,
        }

    def package_model_update(
        self,
        round_id: str,
        local_version: Optional[str] = None,
        artifact_dir: str = "storage/models/updates",
    ) -> Dict[str, Any]:
        """
        Packages model update adhering strictly to the F9-P1 payload schema.
        Writes serialized JSON artifact to artifact_dir and returns the update dict.
        """
        if self.current_model is None or not self.current_model.is_fitted:
            raise RuntimeError(f"Node {self.node_id} has not completed local training.")

        weights, intercept = self.current_model.get_weights()
        loc_version = local_version or f"{self.node_id.lower()}-r{round_id.replace('ROUND-', '').lstrip('0') or '1'}"

        os.makedirs(artifact_dir, exist_ok=True)
        filename = f"{self.node_id.lower()}_{round_id.lower()}.json"
        artifact_ref = os.path.join(artifact_dir, filename).replace("\\", "/")

        payload = {
            "nodeId": self.node_id,
            "roundId": round_id,
            "baseModelVersion": self.base_model_version,
            "localModelVersion": loc_version,
            "sampleCount": self.latest_sample_count,
            "metrics": self.latest_metrics,
            "weights": weights,
            "intercept": intercept,
            "artifactReference": artifact_ref,
            "submittedAt": datetime.now(timezone.utc).isoformat(),
        }

        with open(artifact_ref, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2)

        return payload

    def send_heartbeat(
        self,
        status: str = "ONLINE",
        model_version: Optional[str] = None,
    ) -> Optional[Dict[str, Any]]:
        """
        Transmits node liveness heartbeat to Spring Boot Control Plane.
        """
        if self.network_client is not None:
            return self.network_client.send_heartbeat(
                node_id=self.node_id,
                status=status,
                model_version=model_version or self.base_model_version,
            )
        return None

    def pull_active_model(self) -> Optional[Dict[str, Any]]:
        """
        Pulls active global model metadata and parameters from control plane.
        """
        if self.network_client is not None:
            active_info = self.network_client.get_active_model()
            if active_info and "modelVersion" in active_info:
                self.base_model_version = active_info["modelVersion"]
            return active_info
        return None

    def submit_update_to_control_plane(
        self,
        round_id: str,
        local_version: Optional[str] = None,
        artifact_dir: str = "storage/models/updates",
    ) -> Dict[str, Any]:
        """
        Packages model update locally and transmits parameter payload over REST API.
        """
        payload = self.package_model_update(round_id, local_version, artifact_dir)
        if self.network_client is not None:
            api_res = self.network_client.submit_update(
                round_id=round_id,
                node_id=self.node_id,
                base_model_version=self.base_model_version,
                local_model_version=payload["localModelVersion"],
                sample_count=payload["sampleCount"],
                metrics=payload["metrics"],
                weights=payload["weights"],
                artifact_reference=payload["artifactReference"],
            )
            return api_res
        return payload
