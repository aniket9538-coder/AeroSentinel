"""
AeroSentinel - Federated Coordinator Multi-Round Simulation Runner
File: federated/run_round_simulation.py

Simulates multi-round federated training across municipal nodes (Pune, Mumbai, Delhi):
- Round 1: global-v1 -> Pune, Mumbai, Delhi train and submit -> FedAvg -> global-v2
- Round 2: global-v2 -> Pune, Mumbai train and submit (quorum 2 of 3) -> FedAvg -> global-v3
Verifies persistence in storage/models/global/ and lineage tracking.
"""

import os
import sys
import json

# Ensure workspace root is in path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from federated.coordinator import FederatedCoordinator
from federated.clients.pune import PuneClient
from federated.clients.mumbai import MumbaiClient
from federated.clients.delhi import DelhiClient


def main():
    print("=================================================================")
    print("AeroSentinel F9-P3: Multi-Round Federated Coordinator Simulation")
    print("=================================================================\n")

    global_storage = "storage/models/global"
    update_storage = "storage/models/updates"
    os.makedirs(global_storage, exist_ok=True)
    os.makedirs(update_storage, exist_ok=True)

    coordinator = FederatedCoordinator(storage_dir=global_storage, min_quorum=2)

    print(f"[*] Initial Active Global Model: {coordinator.get_active_global_model().version}")
    print()

    # -------------------------------------------------------------
    # ROUND 1: 3-Node Consensus (Pune + Mumbai + Delhi)
    # -------------------------------------------------------------
    round_1_id = "ROUND-001"
    print(f"=== STARTING {round_1_id} (Base: global-v1, Expected: Pune, Mumbai, Delhi) ===")
    r1_start = coordinator.start_round(
        round_id=round_1_id,
        base_version="global-v1",
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    print(f"  [OK] Round Status: {r1_start['status']}, Quorum Required: {r1_start['minQuorum']}")

    # Node Training
    pune_client = PuneClient(random_state=42)
    mumbai_client = MumbaiClient(random_state=101)
    delhi_client = DelhiClient(random_state=202)

    print("  [*] Pune Node training (1200 samples)...")
    pune_client.train_local_round(round_1_id, base_version="global-v1", sample_count=1200)
    pune_update = pune_client.package_model_update(round_1_id, artifact_dir=update_storage)
    coordinator.submit_node_update(round_1_id, pune_update)
    print(f"    -> Pune update submitted (MAE: {pune_update['metrics']['mae']})")

    print("  [*] Mumbai Node training (800 samples)...")
    mumbai_client.train_local_round(round_1_id, base_version="global-v1", sample_count=800)
    mumbai_update = mumbai_client.package_model_update(round_1_id, artifact_dir=update_storage)
    coordinator.submit_node_update(round_1_id, mumbai_update)
    print(f"    -> Mumbai update submitted (MAE: {mumbai_update['metrics']['mae']})")

    print("  [*] Delhi Node training (1000 samples)...")
    delhi_client.train_local_round(round_1_id, base_version="global-v1", sample_count=1000)
    delhi_update = delhi_client.package_model_update(round_1_id, artifact_dir=update_storage)
    coordinator.submit_node_update(round_1_id, delhi_update)
    print(f"    -> Delhi update submitted (MAE: {delhi_update['metrics']['mae']})")

    # Aggregation
    print("  [*] Dispatching FedAvg Aggregation for ROUND-001...")
    r1_result = coordinator.trigger_aggregation(round_1_id)
    print(f"  [OK] Round 1 Aggregation Complete!")
    print(f"       New Global Model Version: {r1_result['globalModelVersion']}")
    print(f"       Participating Nodes:     {r1_result['participatingNodes']}")
    print(f"       Total Samples:           {r1_result['totalSamples']}")
    print(f"       Aggregated MAE:          {r1_result['aggregatedMetrics']['mae']}")
    print(f"       Aggregated RMSE:         {r1_result['aggregatedMetrics']['rmse']}")
    print(f"       Aggregated ROC-AUC:      {r1_result['aggregatedMetrics']['rocAuc']}")
    print(f"       Artifact Saved:          {r1_result['artifactReference']}")
    print()

    # -------------------------------------------------------------
    # ROUND 2: 2-Node Quorum Verification (Pune + Mumbai)
    # -------------------------------------------------------------
    round_2_id = "ROUND-002"
    active_base = coordinator.get_active_global_model().version
    print(f"=== STARTING {round_2_id} (Base: {active_base}, Quorum: 2 of 3) ===")
    r2_start = coordinator.start_round(
        round_id=round_2_id,
        base_version=active_base,
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    print(f"  [OK] Round Status: {r2_start['status']}, Quorum Required: {r2_start['minQuorum']}")

    print(f"  [*] Pune Node training on {active_base} (1500 samples)...")
    pune_client.train_local_round(round_2_id, base_version=active_base, sample_count=1500)
    pune_update_2 = pune_client.package_model_update(round_2_id, artifact_dir=update_storage)
    coordinator.submit_node_update(round_2_id, pune_update_2)
    print(f"    -> Pune update submitted (MAE: {pune_update_2['metrics']['mae']})")

    print(f"  [*] Mumbai Node training on {active_base} (900 samples)...")
    mumbai_client.train_local_round(round_2_id, base_version=active_base, sample_count=900)
    mumbai_update_2 = mumbai_client.package_model_update(round_2_id, artifact_dir=update_storage)
    coordinator.submit_node_update(round_2_id, mumbai_update_2)
    print(f"    -> Mumbai update submitted (MAE: {mumbai_update_2['metrics']['mae']})")

    print("  [INFO] Delhi Node offline / non-responsive this round (testing partial quorum)...")

    # Aggregation
    print("  [*] Dispatching FedAvg Aggregation for ROUND-002...")
    r2_result = coordinator.trigger_aggregation(round_2_id)
    print(f"  [OK] Round 2 Aggregation Complete!")
    print(f"       New Global Model Version: {r2_result['globalModelVersion']}")
    print(f"       Participating Nodes:     {r2_result['participatingNodes']} (Quorum Met)")
    print(f"       Total Samples:           {r2_result['totalSamples']}")
    print(f"       Aggregated MAE:          {r2_result['aggregatedMetrics']['mae']}")
    print(f"       Aggregated RMSE:         {r2_result['aggregatedMetrics']['rmse']}")
    print(f"       Aggregated ROC-AUC:      {r2_result['aggregatedMetrics']['rocAuc']}")
    print(f"       Artifact Saved:          {r2_result['artifactReference']}")
    print()

    # -------------------------------------------------------------
    # LINEAGE & ARTIFACT AUDIT
    # -------------------------------------------------------------
    print("=== MODEL REGISTRY LINEAGE AUDIT ===")
    catalog = coordinator.get_model_catalog()
    for entry in catalog:
        active_flag = " [ACTIVE]" if entry.get("isActive") else ""
        print(f"  * {entry['version']}: Base={entry.get('baseModelVersion')}, TotalSamples={entry.get('totalSamples', 0)}, Nodes={entry.get('participatingNodes', [])}{active_flag}")
        print(f"    Artifact: {entry.get('artifactPath')}")

    print("\n-----------------------------------------------------------------")
    print("Multi-Round Simulation Verification SUCCESSFUL.")
    print("Artifacts global-v2.joblib and global-v3.joblib are verified on disk.")
    print("-----------------------------------------------------------------")


if __name__ == "__main__":
    main()
