"""
AeroSentinel - E2E Cross-Stack Federated Orchestration & Integration Verification
File: federated/run_e2e_federated_orchestration.py

Executes a live, multi-round cross-stack federated training workflow directly connecting
Python municipal nodes (Pune, Mumbai, Delhi) with the Spring Boot Federated Control Plane
(http://localhost:8080) and PostgreSQL persistence layer (localhost:5432).

Verifies:
1. Control Plane Handshake & Heartbeats
2. Round 1: 3-Node Full Consensus (global-v1 -> global-v2, 3,000 samples)
3. Round 2: Fault-Tolerant Quorum (2 of 3 nodes, global-v2 -> global-v3, 2,400 samples)
4. Guardrails: Stale model rejection (HTTP 400), Duplicate update rejection (HTTP 409), Quorum failure (HTTP 400)
5. Direct PostgreSQL database audit
"""

import sys
import os
import time
import json
import logging
from typing import Dict, List, Any
import psycopg2

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("F9-E2E-Orchestration")

# Ensure repository root is on Python path
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from federated.clients.network_client import (
    FederatedNetworkClient,
    FederatedNetworkError,
    ValidationError,
    DuplicateUpdateError,
    QuorumNotMetError,
)
from federated.clients.pune.client import PuneClient
from federated.clients.mumbai.client import MumbaiClient
from federated.clients.delhi.client import DelhiClient
from federated.coordinator.aggregator import FederatedAggregator
from federated.coordinator.model_registry import ModelRegistry
from federated.models.global_model import GlobalHotspotModel


def print_banner(text: str) -> None:
    width = 80
    print("\n" + "=" * width)
    print(f" {text}".center(width))
    print("=" * width)


def print_section(step_num: int, title: str) -> None:
    print(f"\n--- [STEP {step_num}] {title} ---")


def run_e2e_orchestration():
    print_banner("AEROSENTINEL F9-P5: END-TO-END CROSS-STACK FEDERATED ORCHESTRATION")

    control_plane_url = os.environ.get("CONTROL_PLANE_URL", "http://localhost:8080")
    db_host = os.environ.get("DB_HOST", "localhost")
    db_port = int(os.environ.get("DB_PORT", 5432))
    db_name = os.environ.get("DB_NAME", "aerosentinel")
    db_user = os.environ.get("DB_USER", "aerosentinel_user")
    db_pass = os.environ.get("DB_PASSWORD", "change_this_in_production")

    client = FederatedNetworkClient(base_url=control_plane_url, timeout=15.0)
    aggregator = FederatedAggregator()
    registry = ModelRegistry()

    # =========================================================================
    # STEP 1: Control Plane Handshake & Node Synchronization
    # =========================================================================
    print_section(1, "Control Plane Handshake & Node Synchronization")

    assert client.check_health(), f"Spring Boot Control Plane is not reachable or healthy at {control_plane_url}"
    print(f"  [OK] Spring Boot Control Plane health check: UP ({control_plane_url}/actuator/health)")

    active_model = client.get_active_model()
    print(f"  [OK] Initial active model verified: version={active_model.get('modelVersion')} (is_active=True)")

    nodes = client.list_nodes()
    print(f"  [OK] Registered municipal nodes detected: {len(nodes)}")
    for n in nodes:
        print(f"       * {n.get('nodeId')}: {n.get('nodeName')} [status={n.get('status')}]")

    print("\n  Transmitting fresh liveness heartbeats for all 3 municipal nodes...")
    hb_pune = client.send_heartbeat("PUNE", status="ONLINE", model_version=active_model["modelVersion"])
    hb_mumbai = client.send_heartbeat("MUMBAI", status="ONLINE", model_version=active_model["modelVersion"])
    hb_delhi = client.send_heartbeat("DELHI", status="ONLINE", model_version=active_model["modelVersion"])
    print(f"  [OK] Pune heartbeat: status={hb_pune.get('status')}")
    print(f"  [OK] Mumbai heartbeat: status={hb_mumbai.get('status')}")
    print(f"  [OK] Delhi heartbeat: status={hb_delhi.get('status')}")

    # =========================================================================
    # STEP 2: Round 1 Execution (3-Node Full Consensus: global-v1 -> global-v2)
    # =========================================================================
    print_section(2, "Round 1 Execution (3-Node Full Consensus: global-v1 -> global-v2)")

    round1_id = "ROUND-001"
    print(f"  Creating {round1_id} with baseModelVersion=global-v1, minQuorum=2, nodes=[PUNE, MUMBAI, DELHI]...")
    round1_create = client.create_round(
        round_id=round1_id,
        base_model_version="global-v1",
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    print(f"  [OK] Round 1 created: id={round1_create.get('roundId')}, status={round1_create.get('status')}")

    # Update heartbeats to TRAINING
    client.send_heartbeat("PUNE", status="TRAINING", model_version="global-v1")
    client.send_heartbeat("MUMBAI", status="TRAINING", model_version="global-v1")
    client.send_heartbeat("DELHI", status="TRAINING", model_version="global-v1")

    # Initialize municipal clients
    pune_client = PuneClient(random_state=42, network_client=client)
    mumbai_client = MumbaiClient(random_state=101, network_client=client)
    delhi_client = DelhiClient(random_state=202, network_client=client)

    print("\n  Executing local isolated training across municipal nodes:")
    print("  -> Pune Municipal Node: 1,200 telemetry samples (traffic & industrial features)")
    pune_res1 = pune_client.train_local_round(round_id=round1_id, base_version="global-v1", sample_count=1200)
    pune_up1 = pune_client.package_model_update(round_id=round1_id, local_version="pune-v1")
    res_up1_pune = client.submit_update(
        round_id=round1_id,
        node_id="PUNE",
        base_model_version="global-v1",
        local_model_version="pune-v1",
        sample_count=1200,
        metrics=pune_res1["metrics"],
        weights=pune_up1["weights"],
        artifact_reference=pune_up1["artifactReference"],
    )
    print(f"     [SUBMITTED] Pune update validated by control plane: status={res_up1_pune.get('status')}, samples={res_up1_pune.get('sampleCount')}")

    print("  -> Mumbai Coastal Node: 1,000 telemetry samples (maritime humidity & coastal transport)")
    mumbai_res1 = mumbai_client.train_local_round(round_id=round1_id, base_version="global-v1", sample_count=1000)
    mumbai_up1 = mumbai_client.package_model_update(round_id=round1_id, local_version="mumbai-v1")
    res_up1_mumbai = client.submit_update(
        round_id=round1_id,
        node_id="MUMBAI",
        base_model_version="global-v1",
        local_model_version="mumbai-v1",
        sample_count=1000,
        metrics=mumbai_res1["metrics"],
        weights=mumbai_up1["weights"],
        artifact_reference=mumbai_up1["artifactReference"],
    )
    print(f"     [SUBMITTED] Mumbai update validated by control plane: status={res_up1_mumbai.get('status')}, samples={res_up1_mumbai.get('sampleCount')}")

    print("  -> Delhi Regional Node: 800 telemetry samples (inversion & seasonal PM2.5)")
    delhi_res1 = delhi_client.train_local_round(round_id=round1_id, base_version="global-v1", sample_count=800)
    delhi_up1 = delhi_client.package_model_update(round_id=round1_id, local_version="delhi-v1")
    res_up1_delhi = client.submit_update(
        round_id=round1_id,
        node_id="DELHI",
        base_model_version="global-v1",
        local_model_version="delhi-v1",
        sample_count=800,
        metrics=delhi_res1["metrics"],
        weights=delhi_up1["weights"],
        artifact_reference=delhi_up1["artifactReference"],
    )
    print(f"     [SUBMITTED] Delhi update validated by control plane: status={res_up1_delhi.get('status')}, samples={res_up1_delhi.get('sampleCount')}")

    # Verify round status before aggregation
    round1_detail = client.get_round(round1_id)
    assert round1_detail["receivedUpdatesCount"] == 3, f"Expected 3 updates, got {round1_detail['receivedUpdatesCount']}"
    assert round1_detail["status"] == "UPDATES_COLLECTING"
    print(f"  [OK] Round 1 update collection verified: {round1_detail['receivedUpdatesCount']} updates received (quorum={round1_detail['minQuorum']})")

    # Local Python FedAvg verification & serialization
    fedavg_r1 = aggregator.aggregate([pune_up1, mumbai_up1, delhi_up1])
    assert fedavg_r1["totalSamples"] == 3000
    model_r1 = GlobalHotspotModel(
        version="global-v2",
        weights=fedavg_r1["weights"],
        intercept=fedavg_r1["intercept"],
        metadata={
            "version": "global-v2",
            "roundId": round1_id,
            "baseModelVersion": "global-v1",
            "participatingNodes": ["PUNE", "MUMBAI", "DELHI"],
            "totalSamples": 3000,
            "metrics": fedavg_r1["metrics"],
            "status": "ACTIVE",
        },
    )
    registry.register_model(model_r1, is_active=True)

    print("\n  Triggering sample-weighted FedAvg consensus aggregation in Spring Boot Control Plane...")
    agg_res1 = client.trigger_aggregation(round1_id)
    new_model_r1 = agg_res1.get('globalModelVersion') or agg_res1.get('targetVersion')
    metrics_r1 = agg_res1.get('aggregatedMetrics') or agg_res1.get('metrics', {})
    print(f"  [OK] Aggregation completed: newModel={new_model_r1}, status={agg_res1.get('status')}")
    print(f"       * Total Samples Aggregated: {agg_res1.get('totalSamples')} (Pune: 1,200 [40%], Mumbai: 1,000 [33.3%], Delhi: 800 [26.7%])")
    print(f"       * Convergence Metrics: MAE={metrics_r1.get('mae')}, RMSE={metrics_r1.get('rmse')}, ROC-AUC={metrics_r1.get('rocAuc')}")

    active_model_r1 = client.get_active_model()
    assert active_model_r1["modelVersion"] == "global-v2", f"Expected active model global-v2, got {active_model_r1['modelVersion']}"
    assert active_model_r1["totalSamples"] == 3000
    print(f"  [OK] Active consensus pointer advanced to: {active_model_r1['modelVersion']} (totalSamples={active_model_r1['totalSamples']})")

    # =========================================================================
    # STEP 3: Round 2 Execution (Fault-Tolerant Quorum: 2 of 3 Nodes)
    # =========================================================================
    print_section(3, "Round 2 Execution (Fault-Tolerant Quorum: 2 of 3 Nodes -> global-v3)")

    round2_id = "ROUND-002"
    print(f"  Creating {round2_id} with baseModelVersion=global-v2, minQuorum=2, nodes=[PUNE, MUMBAI, DELHI]...")
    round2_create = client.create_round(
        round_id=round2_id,
        base_model_version="global-v2",
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    print(f"  [OK] Round 2 created: id={round2_create.get('roundId')}, status={round2_create.get('status')}")

    # Synchronize municipal clients with global-v2 parameters
    pune_client.receive_global_model(model_r1.weights, model_r1.intercept, base_version="global-v2")
    mumbai_client.receive_global_model(model_r1.weights, model_r1.intercept, base_version="global-v2")

    print("\n  Executing local round 2 training:")
    print("  -> Pune Municipal Node: 1,300 telemetry samples (warm-started from global-v2)")
    pune_res2 = pune_client.train_local_round(round_id=round2_id, base_version="global-v2", sample_count=1300)
    pune_up2 = pune_client.package_model_update(round_id=round2_id, local_version="pune-v2")
    res_up2_pune = client.submit_update(
        round_id=round2_id,
        node_id="PUNE",
        base_model_version="global-v2",
        local_model_version="pune-v2",
        sample_count=1300,
        metrics=pune_res2["metrics"],
        weights=pune_up2["weights"],
        artifact_reference=pune_up2["artifactReference"],
    )
    print(f"     [SUBMITTED] Pune update validated: status={res_up2_pune.get('status')}, samples={res_up2_pune.get('sampleCount')}")

    print("  -> Mumbai Coastal Node: 1,100 telemetry samples (warm-started from global-v2)")
    mumbai_res2 = mumbai_client.train_local_round(round_id=round2_id, base_version="global-v2", sample_count=1100)
    mumbai_up2 = mumbai_client.package_model_update(round_id=round2_id, local_version="mumbai-v2")
    res_up2_mumbai = client.submit_update(
        round_id=round2_id,
        node_id="MUMBAI",
        base_model_version="global-v2",
        local_model_version="mumbai-v2",
        sample_count=1100,
        metrics=mumbai_res2["metrics"],
        weights=mumbai_up2["weights"],
        artifact_reference=mumbai_up2["artifactReference"],
    )
    print(f"     [SUBMITTED] Mumbai update validated: status={res_up2_mumbai.get('status')}, samples={res_up2_mumbai.get('sampleCount')}")

    print("  -> Delhi Regional Node: Simulating network disconnection / offline status (0 updates submitted)")
    client.send_heartbeat("DELHI", status="OFFLINE", model_version="global-v1")

    # Verify quorum satisfied (2 of 3 updates >= minQuorum 2)
    round2_detail = client.get_round(round2_id)
    assert round2_detail["receivedUpdatesCount"] == 2
    assert round2_detail["minQuorum"] == 2
    print(f"  [OK] Quorum condition verified: 2 updates received >= minQuorum {round2_detail['minQuorum']}")

    # Local Python FedAvg verification & serialization
    fedavg_r2 = aggregator.aggregate([pune_up2, mumbai_up2])
    assert fedavg_r2["totalSamples"] == 2400
    model_r2 = GlobalHotspotModel(
        version="global-v3",
        weights=fedavg_r2["weights"],
        intercept=fedavg_r2["intercept"],
        metadata={
            "version": "global-v3",
            "roundId": round2_id,
            "baseModelVersion": "global-v2",
            "participatingNodes": ["PUNE", "MUMBAI"],
            "totalSamples": 2400,
            "metrics": fedavg_r2["metrics"],
            "status": "ACTIVE",
        },
    )
    registry.register_model(model_r2, is_active=True)

    print("\n  Triggering aggregation with 2-of-3 quorum...")
    agg_res2 = client.trigger_aggregation(round2_id)
    new_model_r2 = agg_res2.get('globalModelVersion') or agg_res2.get('targetVersion')
    print(f"  [OK] Aggregation completed: newModel={new_model_r2}, status={agg_res2.get('status')}")
    print(f"       * Total Samples Aggregated: {agg_res2.get('totalSamples')} (Pune: 1,300 [54.2%], Mumbai: 1,100 [45.8%])")
    print(f"       * Participating Nodes Count: {agg_res2.get('participatingNodesCount')}")

    active_model_r2 = client.get_active_model()
    assert active_model_r2["modelVersion"] == "global-v3"
    assert active_model_r2["totalSamples"] == 2400
    print(f"  [OK] Active consensus pointer advanced to: {active_model_r2['modelVersion']} (totalSamples={active_model_r2['totalSamples']})")

    # =========================================================================
    # STEP 4: Guardrail Validations
    # =========================================================================
    print_section(4, "Guardrail & Negative Testing")

    # 4.1 Stale Model Rejection
    print("  [Guardrail 1] Testing stale model rejection (submitting global-v1 to a round requiring global-v2)...")
    stale_test_id = "ROUND-STALE-TEST"
    client.create_round(
        round_id=stale_test_id,
        base_model_version="global-v2",
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    stale_rejected = False
    try:
        client.submit_update(
            round_id=stale_test_id,
            node_id="DELHI",
            base_model_version="global-v1",  # Stale: round requires global-v2
            local_model_version="delhi-stale",
            sample_count=500,
            metrics={"mae": 0.5, "rmse": 0.6},
        )
    except ValidationError as e:
        stale_rejected = True
        print(f"  [PASSED] HTTP 400 Bad Request received as expected: {e}")
    assert stale_rejected, "Expected ValidationError for stale baseModelVersion"

    # 4.2 Duplicate Update Rejection
    print("  [Guardrail 2] Testing duplicate update rejection (Pune submitting twice to same active round)...")
    dup_test_id = "ROUND-DUP-TEST"
    client.create_round(
        round_id=dup_test_id,
        base_model_version="global-v3",
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    # First submission -> VALIDATED
    res_first = client.submit_update(
        round_id=dup_test_id,
        node_id="PUNE",
        base_model_version="global-v3",
        local_model_version="pune-dup-1",
        sample_count=800,
        metrics={"mae": 0.4},
    )
    assert res_first.get("status") == "VALIDATED"
    print("     First submission succeeded: VALIDATED")

    duplicate_rejected = False
    try:
        client.submit_update(
            round_id=dup_test_id,
            node_id="PUNE",
            base_model_version="global-v3",
            local_model_version="pune-dup-2",
            sample_count=800,
            metrics={"mae": 0.4},
        )
    except DuplicateUpdateError as e:
        duplicate_rejected = True
        print(f"  [PASSED] HTTP 409 Conflict received as expected: {e}")
    except ValidationError as e:
        if "duplicate" in str(e).lower():
            duplicate_rejected = True
            print(f"  [PASSED] Duplicate rejection error received: {e}")
    assert duplicate_rejected, "Expected rejection for duplicate update submission"

    # 4.3 Quorum Failure
    print("  [Guardrail 3] Testing quorum enforcement blocks premature aggregation...")
    quorum_test_id = "ROUND-QUORUM-TEST"
    client.create_round(
        round_id=quorum_test_id,
        base_model_version="global-v3",
        participating_nodes=["PUNE", "MUMBAI", "DELHI"],
        min_quorum=2,
    )
    # Submit only 1 update (Pune)
    client.submit_update(
        round_id=quorum_test_id,
        node_id="PUNE",
        base_model_version="global-v3",
        local_model_version="pune-qtest",
        sample_count=500,
        metrics={"mae": 0.45},
    )
    quorum_blocked = False
    try:
        client.trigger_aggregation(quorum_test_id)
    except (QuorumNotMetError, ValidationError) as e:
        quorum_blocked = True
        print(f"  [PASSED] Aggregation blocked as expected: {e}")
    assert quorum_blocked, "Expected QuorumNotMetError when updates < minQuorum"

    quorum_round_status = client.get_round(quorum_test_id)
    assert quorum_round_status["status"] == "FAILED"
    print(f"  [PASSED] Quorum-failed round status correctly marked: {quorum_round_status['status']}")

    # =========================================================================
    # STEP 5: Direct PostgreSQL Audit
    # =========================================================================
    print_section(5, "PostgreSQL Database Relational Audit")

    conn = psycopg2.connect(
        host=db_host,
        port=db_port,
        dbname=db_name,
        user=db_user,
        password=db_pass,
    )
    cur = conn.cursor()

    # Query federated_rounds
    print("\n  [AUDIT] Querying table `federated_rounds`:")
    cur.execute("SELECT round_id, status, min_quorum, total_samples, target_model_version FROM federated_rounds ORDER BY created_at ASC;")
    db_rounds = cur.fetchall()
    print(f"  {'Round ID':<20} | {'Status':<12} | {'Min Quorum':<10} | {'Total Samples':<14} | {'Target Model':<12}")
    print("  " + "-" * 75)
    for r in db_rounds:
        print(f"  {r[0]:<20} | {r[1]:<12} | {str(r[2]):<10} | {str(r[3]):<14} | {str(r[4]):<12}")

    # Query model_updates
    print("\n  [AUDIT] Querying table `model_updates`:")
    cur.execute("SELECT round_id, node_id, sample_count, status FROM model_updates ORDER BY round_id, node_id;")
    db_updates = cur.fetchall()
    print(f"  {'Round ID':<20} | {'Node ID':<10} | {'Samples':<10} | {'Status':<12}")
    print("  " + "-" * 60)
    for u in db_updates:
        print(f"  {u[0]:<20} | {u[1]:<10} | {str(u[2]):<10} | {str(u[3]):<12}")

    # Query federated_global_models
    print("\n  [AUDIT] Querying table `federated_global_models`:")
    cur.execute("SELECT version, is_active, total_samples, participating_nodes, round_id FROM federated_global_models ORDER BY created_at ASC;")
    db_models = cur.fetchall()
    print(f"  {'Version':<12} | {'Is Active':<10} | {'Samples':<10} | {'Round ID':<20} | {'Participants'}")
    print("  " + "-" * 75)
    for m in db_models:
        print(f"  {m[0]:<12} | {str(m[1]):<10} | {str(m[2]):<10} | {str(m[4]):<20} | {m[3]}")

    cur.close()
    conn.close()

    # Relational Assertions
    round1_db = next((r for r in db_rounds if r[0] == round1_id), None)
    assert round1_db and round1_db[1] == "COMPLETED" and round1_db[3] == 3000

    round2_db = next((r for r in db_rounds if r[0] == round2_id), None)
    assert round2_db and round2_db[1] == "COMPLETED" and round2_db[3] == 2400

    qtest_db = next((r for r in db_rounds if r[0] == quorum_test_id), None)
    assert qtest_db and qtest_db[1] == "FAILED"

    r1_updates = [u for u in db_updates if u[0] == round1_id]
    assert len(r1_updates) == 3, f"Expected 3 updates for {round1_id}, found {len(r1_updates)}"

    r2_updates = [u for u in db_updates if u[0] == round2_id]
    assert len(r2_updates) == 2, f"Expected 2 updates for {round2_id}, found {len(r2_updates)}"

    active_db_models = [m for m in db_models if m[1] is True]
    assert len(active_db_models) == 1, f"Expected exactly 1 active global model, found {len(active_db_models)}"
    assert active_db_models[0][0] == "global-v3", f"Expected active model to be global-v3, got {active_db_models[0][0]}"

    print("\n  [VERIFIED] All PostgreSQL relational integrity constraints and state transitions PASSED.")

    # =========================================================================
    # Catalog Lineage Verification
    # =========================================================================
    print_section(6, "Lineage Catalog Verification")
    catalog = client.get_model_catalog()
    print(f"  [OK] Model lineage catalog retrieved ({len(catalog)} versions):")
    for cat in catalog:
        m_ver = cat.get('modelVersion') or cat.get('version')
        print(f"       * {m_ver}: active={cat.get('isActive')}, round={cat.get('roundId')}, samples={cat.get('totalSamples')}, path={cat.get('artifactPath')}")

    print_banner("F9-P5 E2E CROSS-STACK FEDERATED ORCHESTRATION: ALL PHASES PASSED")
    return True


if __name__ == "__main__":
    success = run_e2e_orchestration()
    sys.exit(0 if success else 1)
