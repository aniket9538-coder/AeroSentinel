"""
AeroSentinel - Federated Coordinator Facade
File: federated/coordinator/coordinator.py

Central orchestration facade uniting the RoundManager, FederatedAggregator,
and ModelRegistry into a cohesive control plane for federated learning rounds.
"""

from typing import Dict, List, Optional, Any
from datetime import datetime, timezone

from federated.coordinator.round_manager import RoundManager, RoundState
from federated.coordinator.aggregator import FederatedAggregator
from federated.coordinator.model_registry import ModelRegistry
from federated.models.global_model import GlobalHotspotModel


class FederatedCoordinator:
    """
    Central coordinator orchestrating round lifecycle, update intake,
    mathematical aggregation, and global model catalog updates.
    """

    def __init__(
        self,
        storage_dir: str = "storage/models/global",
        expected_nodes: Optional[List[str]] = None,
        min_quorum: int = 2,
    ):
        self.round_manager = RoundManager(expected_nodes=expected_nodes, min_quorum=min_quorum)
        self.aggregator = FederatedAggregator()
        self.model_registry = ModelRegistry(storage_dir=storage_dir)

    def start_round(
        self,
        round_id: str,
        base_version: Optional[str] = None,
        participating_nodes: Optional[List[str]] = None,
        min_quorum: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        Starts a new federated training round using the active global model.
        """
        base_model_ver = base_version or self.model_registry.active_version
        return self.round_manager.start_round(
            round_id=round_id,
            base_model_version=base_model_ver,
            participating_nodes=participating_nodes,
            min_quorum=min_quorum,
        )

    def get_active_global_model(self) -> GlobalHotspotModel:
        """Returns the currently active consensus global model."""
        return self.model_registry.get_active_model()

    def submit_node_update(self, round_id: str, update_payload: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validates and records a municipal node update for the active round.
        """
        return self.round_manager.submit_update(round_id=round_id, update_payload=update_payload)

    def trigger_aggregation(
        self,
        round_id: str,
        target_version: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Validates participation quorum, computes FedAvg consensus parameters,
        registers new global model version, and marks round as COMPLETED.
        """
        round_info = self.round_manager.get_round(round_id)
        base_ver = round_info["baseModelVersion"]

        # 1. Quorum check & update extraction
        updates = self.round_manager.prepare_for_aggregation(round_id)

        # 2. Mathematical aggregation via FedAvg
        agg_result = self.aggregator.aggregate(updates)

        # 3. Determine next global model version
        if target_version:
            next_ver = target_version
        else:
            # Auto-increment global-vX
            try:
                base_num = int(base_ver.replace("global-v", ""))
                next_ver = f"global-v{base_num + 1}"
            except Exception:
                next_ver = f"global-{round_id.lower()}"

        # 4. Construct and register new global consensus model
        metadata = {
            "version": next_ver,
            "baseModelVersion": base_ver,
            "roundId": round_id,
            "participatingNodes": agg_result["participatingNodes"],
            "totalSamples": agg_result["totalSamples"],
            "aggregationStrategy": agg_result["aggregationStrategy"],
            "metrics": agg_result["metrics"],
            "createdAt": datetime.now(timezone.utc).isoformat(),
        }

        new_global_model = GlobalHotspotModel(
            version=next_ver,
            weights=agg_result["weights"],
            intercept=agg_result["intercept"],
            metadata=metadata,
        )

        reg_metadata = self.model_registry.register_model(new_global_model, is_active=True)

        # 5. Complete round lifecycle
        self.round_manager.complete_round(
            round_id=round_id,
            target_model_version=next_ver,
            metrics=agg_result["metrics"],
        )

        return {
            "roundId": round_id,
            "status": "COMPLETED",
            "baseModelVersion": base_ver,
            "globalModelVersion": next_ver,
            "participatingNodes": len(agg_result["participatingNodes"]),
            "totalSamples": agg_result["totalSamples"],
            "aggregationStrategy": agg_result["aggregationStrategy"],
            "aggregatedMetrics": agg_result["metrics"],
            "artifactReference": reg_metadata["artifactPath"],
            "completedAt": datetime.now(timezone.utc).isoformat(),
        }

    def get_round_status(self, round_id: str) -> Dict[str, Any]:
        """Returns lifecycle status and summary of specified round."""
        round_info = self.round_manager.get_round(round_id)
        return self.round_manager._format_summary(round_info)

    def get_model_catalog(self) -> List[Dict[str, Any]]:
        """Returns version lineage catalog of all global models."""
        return self.model_registry.list_models()
