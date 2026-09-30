"""
AeroSentinel - Federated Global Model Registry
File: federated/coordinator/model_registry.py

Maintains immutable version lineage, artifact storage, and active model references
for consensus models generated across federated rounds (global-v1 -> global-v2 -> ...).
"""

from typing import Dict, List, Optional, Any
import os
from datetime import datetime, timezone

from federated.models.global_model import GlobalHotspotModel
from federated.models.local_model import FEATURE_COUNT


class ModelRegistry:
    """
    Catalog and persistence manager for global federated models.
    """

    def __init__(self, storage_dir: str = "storage/models/global"):
        self.storage_dir = storage_dir
        os.makedirs(self.storage_dir, exist_ok=True)
        self.models: Dict[str, GlobalHotspotModel] = {}
        self.active_version: str = "global-v1"
        self.lineage: List[Dict[str, Any]] = []

        # Initialize default baseline global-v1 model if not present
        self._ensure_baseline_model()

    def _ensure_baseline_model(self) -> None:
        """Ensures baseline global-v1 exists in registry."""
        baseline_file = os.path.join(self.storage_dir, "global-v1.joblib").replace("\\", "/")
        if os.path.exists(baseline_file):
            try:
                model = GlobalHotspotModel.load(baseline_file)
                self.models["global-v1"] = model
                return
            except Exception:
                pass

        # Create baseline model with zero weights
        baseline_model = GlobalHotspotModel(
            version="global-v1",
            weights=[0.0] * FEATURE_COUNT,
            intercept=0.0,
            metadata={
                "version": "global-v1",
                "roundId": "ROUND-000",
                "baseModelVersion": None,
                "participatingNodes": ["PUNE", "MUMBAI", "DELHI"],
                "totalSamples": 0,
                "aggregationStrategy": "BaselineInitialParameters",
                "metrics": {"mae": 0.5, "rmse": 0.5, "rocAuc": 0.5, "brierScore": 0.25},
                "status": "ACTIVE",
                "createdAt": datetime.now(timezone.utc).isoformat(),
            },
        )
        self.register_model(baseline_model, is_active=True)

    def register_model(
        self,
        model: GlobalHotspotModel,
        is_active: bool = True,
    ) -> Dict[str, Any]:
        """
        Persists global model artifact to disk and records metadata in registry catalog.
        """
        version = model.version
        artifact_path = os.path.join(self.storage_dir, f"{version}.joblib").replace("\\", "/")
        model.save(artifact_path)

        self.models[version] = model
        if is_active:
            self.active_version = version

        metadata = model.get_metadata()
        metadata["artifactPath"] = artifact_path
        metadata["status"] = "ACTIVE" if is_active else "SUPERSEDED"
        metadata["registeredAt"] = datetime.now(timezone.utc).isoformat()

        # Update previous active entries in lineage
        for item in self.lineage:
            if is_active and item.get("version") != version:
                item["status"] = "SUPERSEDED"

        self.lineage.append(metadata)
        return metadata

    def get_model(self, version: Optional[str] = None) -> GlobalHotspotModel:
        """Retrieves global model by version (or currently active version)."""
        target_version = version or self.active_version
        if target_version in self.models:
            return self.models[target_version]

        artifact_path = os.path.join(self.storage_dir, f"{target_version}.joblib").replace("\\", "/")
        if os.path.exists(artifact_path):
            model = GlobalHotspotModel.load(artifact_path)
            self.models[target_version] = model
            return model

        raise KeyError(f"Global model version '{target_version}' not found in registry.")

    def get_active_model(self) -> GlobalHotspotModel:
        """Returns the currently active consensus model."""
        return self.get_model(self.active_version)

    def set_active_version(self, version: str) -> Dict[str, Any]:
        """Designates specified model version as the active global model."""
        # Ensure model exists
        model = self.get_model(version)
        self.active_version = version

        for item in self.lineage:
            if item.get("version") == version:
                item["status"] = "ACTIVE"
            else:
                item["status"] = "SUPERSEDED"

        return model.get_metadata()

    def list_models(self) -> List[Dict[str, Any]]:
        """Returns ordered list of all model versions in lineage."""
        return self.lineage.copy()
