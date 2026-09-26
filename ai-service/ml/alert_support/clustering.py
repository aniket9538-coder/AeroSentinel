"""
AeroSentinel - Spatio-Temporal Event Clustering Module
File: ai-service/ml/alert_support/clustering.py
Fulfills Section 18 (Spatial Consistency) & Section 33 (Event Deduplication).
"""

from typing import List, Dict, Any, Set
import h3


class EventClusterer:
    """Groups adjacent H3 cells with concurrent hotspot elevations into a single unified event cluster."""

    @classmethod
    def group_cells(cls, primary_cell: str, neighbor_payloads: List[Dict[str, Any]]) -> List[str]:
        cluster_cells: Set[str] = {primary_cell}
        if not neighbor_payloads:
            return list(cluster_cells)

        try:
            k1_neighbors = set(h3.grid_disk(primary_cell, 1))
        except Exception:
            k1_neighbors = set()

        for np in neighbor_payloads:
            n_cell = np.get("h3_cell_id")
            is_hot = np.get("hotspot", {}).get("is_hotspot", False)
            if n_cell and is_hot and (n_cell in k1_neighbors):
                cluster_cells.add(n_cell)

        return sorted(list(cluster_cells))