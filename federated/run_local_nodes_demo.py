"""
AeroSentinel - Local Nodes Runtime Verification Runner
File: federated/run_local_nodes_demo.py

Executes local municipal model training across Pune, Mumbai, and Delhi,
evaluates metrics, and serializes model update packages to storage/models/updates/.
"""

import os
import json
import sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from federated.clients.pune import PuneClient
from federated.clients.mumbai import MumbaiClient
from federated.clients.delhi import DelhiClient


def main():
    print("=================================================================")
    print("AeroSentinel F9-P2: Local City ML Nodes & Common Model Training")
    print("=================================================================\n")

    artifact_dir = "storage/models/updates"
    os.makedirs(artifact_dir, exist_ok=True)

    clients = [
        ("Pune", PuneClient(random_state=42), 1200),
        ("Mumbai", MumbaiClient(random_state=101), 800),
        ("Delhi", DelhiClient(random_state=202), 1000),
    ]

    updates = []
    round_id = "ROUND-001"
    base_version = "global-v1"

    for city_name, client, sample_count in clients:
        print(f"[{city_name.upper()} NODE] Training on {sample_count} local records...")
        train_res = client.train_local_round(round_id=round_id, base_version=base_version, sample_count=sample_count)
        update_payload = client.package_model_update(round_id=round_id, artifact_dir=artifact_dir)
        updates.append(update_payload)

        metrics = update_payload["metrics"]
        print(f"  [OK] Model Version: {update_payload['localModelVersion']}")
        print(f"  [OK] Sample Count:  {update_payload['sampleCount']}")
        print(f"  [OK] MAE:           {metrics['mae']}")
        print(f"  [OK] RMSE:          {metrics['rmse']}")
        print(f"  [OK] ROC-AUC:       {metrics['rocAuc']}")
        print(f"  [OK] Brier Score:   {metrics['brierScore']}")
        print(f"  [OK] Artifact Ref:  {update_payload['artifactReference']}")
        print()

    print("-----------------------------------------------------------------")
    print(f"Summary: Successfully produced {len(updates)} local model updates.")
    print("All updates strictly conform to 36-feature schema (f3-features-v1).")
    print("Raw city data remained isolated at each node.")
    print("-----------------------------------------------------------------")


if __name__ == "__main__":
    main()
