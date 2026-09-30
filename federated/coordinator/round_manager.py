"""
AeroSentinel - Federated Round Manager
File: federated/coordinator/round_manager.py

Manages federated round state machine, node enrollment, update validation,
and minimum participation quorum enforcement (e.g., minimum 2 of 3 updates).
"""

from typing import Dict, List, Optional, Any
from enum import Enum
from datetime import datetime, timezone

from federated.models.local_model import FEATURE_COUNT


class RoundState(str, Enum):
    CREATED = "CREATED"
    MODEL_DISTRIBUTED = "MODEL_DISTRIBUTED"
    TRAINING = "TRAINING"
    UPDATES_COLLECTING = "UPDATES_COLLECTING"
    AGGREGATING = "AGGREGATING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


DEFAULT_EXPECTED_NODES = ["PUNE", "MUMBAI", "DELHI"]
DEFAULT_MIN_QUORUM = 2


class RoundManager:
    """
    Coordinates lifecycle transitions and update validation for federated rounds.
    """

    def __init__(self, expected_nodes: Optional[List[str]] = None, min_quorum: int = DEFAULT_MIN_QUORUM):
        self.default_expected_nodes = expected_nodes or DEFAULT_EXPECTED_NODES
        self.default_min_quorum = min_quorum
        self.rounds: Dict[str, Dict[str, Any]] = {}

    def start_round(
        self,
        round_id: str,
        base_model_version: str,
        participating_nodes: Optional[List[str]] = None,
        min_quorum: Optional[int] = None,
    ) -> Dict[str, Any]:
        """
        Initializes a new federated training round.
        """
        if round_id in self.rounds:
            existing_state = self.rounds[round_id]["status"]
            if existing_state not in [RoundState.COMPLETED, RoundState.FAILED]:
                raise ValueError(f"Round {round_id} already exists in active state {existing_state}.")

        nodes = participating_nodes or self.default_expected_nodes
        quorum = min_quorum or self.default_min_quorum

        round_data = {
            "roundId": round_id,
            "baseModelVersion": base_model_version,
            "targetModelVersion": None,
            "status": RoundState.CREATED,
            "participatingNodes": nodes.copy(),
            "minQuorum": quorum,
            "updates": {},  # nodeId -> update_payload
            "startedAt": datetime.now(timezone.utc).isoformat(),
            "completedAt": None,
            "failureReason": None,
        }

        self.rounds[round_id] = round_data
        return self._format_summary(round_data)

    def distribute_model(self, round_id: str) -> Dict[str, Any]:
        """Transitions round to MODEL_DISTRIBUTED and TRAINING."""
        r = self._get_round(round_id)
        if r["status"] != RoundState.CREATED:
            raise ValueError(f"Cannot distribute model from state {r['status']}")

        r["status"] = RoundState.MODEL_DISTRIBUTED
        r["status"] = RoundState.TRAINING
        return self._format_summary(r)

    def submit_update(self, round_id: str, update_payload: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validates and records a municipal model update for the round.
        """
        r = self._get_round(round_id)

        # 1. State check
        allowed_states = [
            RoundState.CREATED,
            RoundState.MODEL_DISTRIBUTED,
            RoundState.TRAINING,
            RoundState.UPDATES_COLLECTING,
        ]
        if r["status"] not in allowed_states:
            raise ValueError(f"Round {round_id} is in state {r['status']}, not accepting updates.")

        node_id = update_payload.get("nodeId")
        if not node_id:
            raise ValueError("Update payload missing 'nodeId'.")

        # 2. Node enrollment check
        if node_id not in r["participatingNodes"]:
            raise ValueError(f"Node {node_id} is not enrolled in round {round_id}.")

        # 3. Duplicate update check
        if node_id in r["updates"]:
            raise ValueError(f"Duplicate update: Node {node_id} has already submitted an update for round {round_id}.")

        # 4. Stale base model version check
        update_base = update_payload.get("baseModelVersion")
        if update_base != r["baseModelVersion"]:
            raise ValueError(
                f"Stale update from {node_id}: update baseModelVersion '{update_base}' "
                f"does not match round baseModelVersion '{r['baseModelVersion']}'."
            )

        # 5. Weights dimension & integrity check
        weights = update_payload.get("weights")
        if weights is None or len(weights) != FEATURE_COUNT:
            actual_len = len(weights) if weights is not None else 0
            raise ValueError(f"Node {node_id} weights length {actual_len} != {FEATURE_COUNT}.")

        # Record valid update
        r["updates"][node_id] = update_payload
        r["status"] = RoundState.UPDATES_COLLECTING

        return {
            "roundId": round_id,
            "nodeId": node_id,
            "status": "ACCEPTED",
            "receivedUpdatesCount": len(r["updates"]),
            "quorumMet": len(r["updates"]) >= r["minQuorum"],
        }

    def can_aggregate(self, round_id: str) -> bool:
        """Checks if received updates satisfy the minimum participation quorum."""
        r = self._get_round(round_id)
        return len(r["updates"]) >= r["minQuorum"]

    def prepare_for_aggregation(self, round_id: str) -> List[Dict[str, Any]]:
        """
        Transitions round to AGGREGATING and returns collected update payloads.
        Raises ValueError if quorum is not satisfied.
        """
        r = self._get_round(round_id)
        received_count = len(r["updates"])
        required_quorum = r["minQuorum"]

        if received_count < required_quorum:
            r["status"] = RoundState.FAILED
            r["failureReason"] = f"Insufficient quorum: received {received_count} updates, required {required_quorum}."
            raise ValueError(r["failureReason"])

        r["status"] = RoundState.AGGREGATING
        return list(r["updates"].values())

    def complete_round(
        self,
        round_id: str,
        target_model_version: str,
        metrics: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Marks round as COMPLETED with new global model version.
        """
        r = self._get_round(round_id)
        r["status"] = RoundState.COMPLETED
        r["targetModelVersion"] = target_model_version
        r["completedAt"] = datetime.now(timezone.utc).isoformat()
        if metrics:
            r["aggregatedMetrics"] = metrics

        return self._format_summary(r)

    def fail_round(self, round_id: str, reason: str) -> Dict[str, Any]:
        """Marks round as FAILED with error reason."""
        r = self._get_round(round_id)
        r["status"] = RoundState.FAILED
        r["failureReason"] = reason
        r["completedAt"] = datetime.now(timezone.utc).isoformat()
        return self._format_summary(r)

    def get_round(self, round_id: str) -> Dict[str, Any]:
        """Returns complete round details."""
        return self._get_round(round_id)

    def _get_round(self, round_id: str) -> Dict[str, Any]:
        if round_id not in self.rounds:
            raise KeyError(f"Round '{round_id}' does not exist.")
        return self.rounds[round_id]

    def _format_summary(self, r: Dict[str, Any]) -> Dict[str, Any]:
        return {
            "roundId": r["roundId"],
            "baseModelVersion": r["baseModelVersion"],
            "targetModelVersion": r["targetModelVersion"],
            "status": r["status"].value if isinstance(r["status"], RoundState) else r["status"],
            "participatingNodes": r["participatingNodes"],
            "receivedUpdatesCount": len(r["updates"]),
            "minQuorum": r["minQuorum"],
            "startedAt": r["startedAt"],
            "completedAt": r["completedAt"],
            "failureReason": r.get("failureReason"),
        }
