"""
AeroSentinel - Federated Aggregator Engine
File: federated/coordinator/aggregator.py

Implements Sample-Weighted Federated Averaging (FedAvg) over 36-feature weight vectors
and weighted aggregation of validation performance metrics across municipal updates.
"""

from typing import Dict, List, Any, Tuple
import numpy as np

from federated.models.local_model import FEATURE_COUNT


class FederatedAggregator:
    """
    Executes sample-weighted parameter averaging and metrics combination.
    """

    def __init__(self, feature_count: int = FEATURE_COUNT):
        self.feature_count = feature_count

    def aggregate(self, updates: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Aggregates a list of valid node updates using FedAvg.

        Each update must contain:
          - nodeId: str
          - sampleCount: int
          - weights: List[float] (length 36)
          - intercept: float
          - metrics: Dict[str, float] (mae, rmse, rocAuc, ...)
        """
        if not updates:
            raise ValueError("Cannot aggregate empty list of updates.")

        total_samples = 0
        node_ids = []

        # Validate all updates before mathematical combination
        for u in updates:
            node_id = u.get("nodeId")
            samples = u.get("sampleCount", 0)
            weights = u.get("weights")
            intercept = u.get("intercept")

            if not node_id:
                raise ValueError("Update payload missing 'nodeId'.")
            if samples <= 0:
                raise ValueError(f"Invalid sampleCount {samples} from node {node_id}. Must be > 0.")
            if weights is None or len(weights) != self.feature_count:
                actual_len = len(weights) if weights is not None else 0
                raise ValueError(f"Node {node_id} submitted {actual_len} weights, expected {self.feature_count}.")

            weights_arr = np.asarray(weights, dtype=float)
            if np.isnan(weights_arr).any() or np.isinf(weights_arr).any():
                raise ValueError(f"Node {node_id} submitted weights containing NaN or Inf.")

            if intercept is None or np.isnan(float(intercept)) or np.isinf(float(intercept)):
                raise ValueError(f"Node {node_id} submitted invalid intercept: {intercept}")

            total_samples += samples
            node_ids.append(node_id)

        if total_samples <= 0:
            raise ValueError(f"Total aggregated samples must be > 0, got {total_samples}")

        # Compute sample-weighted FedAvg parameter weights and intercept
        aggregated_weights = np.zeros(self.feature_count, dtype=float)
        aggregated_intercept = 0.0

        # Weighted metrics accumulators
        weighted_mae = 0.0
        weighted_rmse_sq = 0.0
        weighted_roc_auc = 0.0
        weighted_brier = 0.0

        for u in updates:
            n_k = u["sampleCount"]
            weight_ratio = n_k / total_samples

            w_k = np.asarray(u["weights"], dtype=float)
            b_k = float(u["intercept"])

            aggregated_weights += weight_ratio * w_k
            aggregated_intercept += weight_ratio * b_k

            metrics = u.get("metrics", {})
            weighted_mae += weight_ratio * metrics.get("mae", 0.0)
            weighted_rmse_sq += weight_ratio * (metrics.get("rmse", 0.0) ** 2)
            weighted_roc_auc += weight_ratio * metrics.get("rocAuc", 0.5)
            weighted_brier += weight_ratio * metrics.get("brierScore", 0.0)

        aggregated_rmse = np.sqrt(weighted_rmse_sq)

        return {
            "weights": [round(float(w), 6) for w in aggregated_weights],
            "intercept": round(float(aggregated_intercept), 6),
            "totalSamples": total_samples,
            "participatingNodes": node_ids,
            "metrics": {
                "mae": round(float(weighted_mae), 4),
                "rmse": round(float(aggregated_rmse), 4),
                "rocAuc": round(float(weighted_roc_auc), 4),
                "brierScore": round(float(weighted_brier), 4),
            },
            "aggregationStrategy": "SampleWeightedFederatedAveraging",
        }
