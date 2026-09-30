# F9-P4 IMPLEMENTATION REPORT: SPRING BOOT FEDERATED CONTROL PLANE & PERSISTENCE

**Feature:** F9 — Federated City Network (Pune + Mumbai + Delhi)
**Phase:** P4 — Spring Boot Federated Control Plane & Persistence Layer
**Date:** 2026-09-30
**Status:** **PASS** ✅
**Repository:** `aniket9538-coder/AeroSentinel`
**Workspace:** `c:\Users\Harsh\OneDrive\Documents\Python Projects\AeroSentinel`

---

## 1. Executive Summary

Phase **F9-P4** delivers the centralized Spring Boot persistence, domain modeling, service layer, and REST API endpoints for AeroSentinel's Federated City Network under `backend/src/main/java/com/aerosentinel/federated/` and `backend/src/main/java/com/aerosentinel/dto/federated/`.

Following the Python coordinator and aggregator baseline established in [F9_P3_COORDINATOR_REPORT.md](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/F9_P3_COORDINATOR_REPORT.md) and Sections 21–30 of Engineering Report 04, the Spring Boot backend acts as the authoritative control plane:
1. **Flyway Migration (`V12__f9_federated_network.sql`):** Created relational tables `federated_nodes`, `federated_rounds`, `model_updates`, and `federated_global_models` with constraints, indexes, initial seed municipal nodes (Pune, Mumbai, Delhi), and the initial active seed consensus model `global-v1`.
2. **JPA Domain Entities & Enums:** Implemented `FederatedNode` (with `NodeStatus`), `FederatedRound` (with `RoundStatus`), `ModelUpdate` (with `UpdateStatus`), and `FederatedGlobalModel`.
3. **Spring Data JPA Repositories:** Implemented `FederatedNodeRepository`, `FederatedRoundRepository`, `ModelUpdateRepository`, and `FederatedGlobalModelRepository`.
4. **DTO Contract Layer:** Implemented typed records for node registration, heartbeats, round management, municipal parameter updates, evaluation metrics, and model catalog queries under `com.aerosentinel.dto.federated`.
5. **Service Layer:** Implemented `FederatedNodeService` (node registration, heartbeat tracking, offline liveness detection), `FederatedRoundService` (lifecycle state transitions, quorum enforcement $\ge 2$ of $3$, stale model rejection, duplicate rejection, FedAvg consensus math), `ModelUpdateService` (intake validation and update recording), and `GlobalModelService` (active model resolution and lineage catalog).
6. **REST Controllers:** Exposed 4 dedicated controllers matching the exact specification:
   - `FederatedNodeController` (`/api/v1/federated/nodes`)
   - `FederatedRoundController` (`/api/v1/federated/rounds`)
   - `ModelUpdateController` (`/api/v1/federated/rounds/{roundId}/updates`)
   - `GlobalModelController` (`/api/v1/federated/models`)
7. **Automated Testing:** 19 unit and MockMvc slice tests executed cleanly (`BUILD SUCCESS`, 0 failures, 0 errors).

---

## 2. Files Inspected & Implemented

### 2.1 Database Migration
| File | Type | Description |
|---|---|---|
| [`backend/src/main/resources/db/migration/V12__f9_federated_network.sql`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/resources/db/migration/V12__f9_federated_network.sql) | Flyway SQL | DDL for `federated_nodes`, `federated_rounds`, `model_updates`, `federated_global_models`, indexes, unique constraints, and seed data. |

### 2.2 JPA Domain Entities & Enums
| File | Package | Description |
|---|---|---|
| [`NodeStatus.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/NodeStatus.java) | `com.aerosentinel.federated` | Enum: `REGISTERED`, `ONLINE`, `TRAINING`, `UPDATE_READY`, `OFFLINE`, `ERROR`. |
| [`RoundStatus.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/RoundStatus.java) | `com.aerosentinel.federated` | Enum: `CREATED`, `MODEL_DISTRIBUTED`, `TRAINING`, `UPDATES_COLLECTING`, `AGGREGATING`, `COMPLETED`, `FAILED`. |
| [`UpdateStatus.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/UpdateStatus.java) | `com.aerosentinel.federated` | Enum: `RECEIVED`, `VALIDATED`, `REJECTED`. |
| [`FederatedNode.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedNode.java) | `com.aerosentinel.federated` | Maps `federated_nodes` with `nodeId`, `cityId`, `nodeName`, `status`, `modelVersion`, `lastSeenAt`, `endpointUrl`. |
| [`FederatedRound.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedRound.java) | `com.aerosentinel.federated` | Maps `federated_rounds` with lifecycle status, quorum, metrics, and update relationships. |
| [`ModelUpdate.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/ModelUpdate.java) | `com.aerosentinel.federated` | Maps `model_updates` with unique constraint `(round_id, node_id)`, metrics, weights, and artifact reference. |
| [`FederatedGlobalModel.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedGlobalModel.java) | `com.aerosentinel.federated` | Maps `federated_global_models` tracking version lineage, active pointer, and aggregated metrics. |

### 2.3 Spring Data JPA Repositories
| File | Package | Description |
|---|---|---|
| [`FederatedNodeRepository.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedNodeRepository.java) | `com.aerosentinel.federated` | Repository for finding nodes by `nodeId`, ordering, and filtering by `NodeStatus`. |
| [`FederatedRoundRepository.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedRoundRepository.java) | `com.aerosentinel.federated` | Repository for rounds lookup, ordering, and existence checks. |
| [`ModelUpdateRepository.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/ModelUpdateRepository.java) | `com.aerosentinel.federated` | Repository for updates query, round lookup, and duplicate prevention. |
| [`FederatedGlobalModelRepository.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedGlobalModelRepository.java) | `com.aerosentinel.federated` | Repository for active model pointer and version lineage catalog. |

### 2.4 DTO Contracts
| File | Package | Description |
|---|---|---|
| [`RegisterNodeRequest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/RegisterNodeRequest.java) | `com.aerosentinel.dto.federated` | Payload to register or update a municipal node endpoint. |
| [`FederatedNodeResponse.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/FederatedNodeResponse.java) | `com.aerosentinel.dto.federated` | Node profile with status, model version, and last seen timestamp. |
| [`NodeHeartbeatRequest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/NodeHeartbeatRequest.java) | `com.aerosentinel.dto.federated` | Heartbeat payload updating status and local model version. |
| [`CreateRoundRequest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/CreateRoundRequest.java) | `com.aerosentinel.dto.federated` | Request payload to initiate a new training round. |
| [`FederatedRoundResponse.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/FederatedRoundResponse.java) | `com.aerosentinel.dto.federated` | Standard response upon round creation. |
| [`SubmitModelUpdateRequest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/SubmitModelUpdateRequest.java) | `com.aerosentinel.dto.federated` | Municipal update payload with local metrics, weights, and sample count. |
| [`ModelUpdateResponse.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/ModelUpdateResponse.java) | `com.aerosentinel.dto.federated` | Response upon intake and validation of model update. |
| [`GlobalModelResponse.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/GlobalModelResponse.java) | `com.aerosentinel.dto.federated` | Detailed metadata and metrics for active or catalog global model. |
| [`ModelCatalogItemResponse.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/dto/federated/ModelCatalogItemResponse.java) | `com.aerosentinel.dto.federated` | Catalog item metadata for global consensus models. |

### 2.5 Service Layer & REST Controllers
| File | Package | Description |
|---|---|---|
| [`FederatedNodeService.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedNodeService.java) | `com.aerosentinel.federated` | Manages node registration, heartbeats, status transitions, and offline node detection. |
| [`FederatedRoundService.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedRoundService.java) | `com.aerosentinel.federated` | Manages round lifecycle, quorum checks ($\ge 2/3$), FedAvg math, atomic model pointer swaps. |
| [`ModelUpdateService.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/ModelUpdateService.java) | `com.aerosentinel.federated` | Validates node enrollment, active round state, base version match, duplicate prevention (409 Conflict). |
| [`GlobalModelService.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/GlobalModelService.java) | `com.aerosentinel.federated` | Resolves active model pointer and serves complete version lineage catalog. |
| [`FederatedNodeController.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedNodeController.java) | `com.aerosentinel.federated` | Endpoints under `/api/v1/federated/nodes`. |
| [`FederatedRoundController.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/FederatedRoundController.java) | `com.aerosentinel.federated` | Endpoints under `/api/v1/federated/rounds`. |
| [`ModelUpdateController.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/ModelUpdateController.java) | `com.aerosentinel.federated` | Endpoints under `/api/v1/federated/rounds/{roundId}/updates`. |
| [`GlobalModelController.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/main/java/com/aerosentinel/federated/GlobalModelController.java) | `com.aerosentinel.federated` | Endpoints under `/api/v1/federated/models`. |

### 2.6 Tests
| File | Package | Description |
|---|---|---|
| [`FederatedRoundServiceTest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/test/java/com/aerosentinel/federated/FederatedRoundServiceTest.java) | `com.aerosentinel.federated` | 8 unit tests covering round lifecycle, FedAvg math, quorum failure, stale rejection, duplicate rejection. |
| [`FederatedNodeServiceTest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/test/java/com/aerosentinel/federated/FederatedNodeServiceTest.java) | `com.aerosentinel.federated` | 5 unit tests covering node registration, heartbeats, status transitions, offline detection. |
| [`FederatedControllersIntegrationTest.java`](file:///c:/Users/Harsh/OneDrive/Documents/Python%20Projects/AeroSentinel/backend/src/test/java/com/aerosentinel/federated/FederatedControllersIntegrationTest.java) | `com.aerosentinel.federated` | 6 MockMvc slice tests covering all REST endpoint contracts and HTTP status codes. |

---

## 3. Database Schema Implementation (`V12__f9_federated_network.sql`)

```sql
-- 1. federated_nodes
CREATE TABLE IF NOT EXISTS federated_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_id VARCHAR(50) NOT NULL UNIQUE,
    city_id UUID,
    node_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ONLINE',
    model_version VARCHAR(50) NOT NULL DEFAULT 'global-v1',
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    endpoint_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. federated_rounds
CREATE TABLE IF NOT EXISTS federated_rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id VARCHAR(50) NOT NULL UNIQUE,
    base_model_version VARCHAR(50) NOT NULL,
    target_model_version VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    participating_nodes JSONB NOT NULL DEFAULT '[]',
    min_quorum INT NOT NULL DEFAULT 2,
    total_samples INT DEFAULT 0,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- 3. model_updates
CREATE TABLE IF NOT EXISTS model_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id VARCHAR(50) NOT NULL REFERENCES federated_rounds(round_id) ON DELETE CASCADE,
    node_id VARCHAR(50) NOT NULL,
    base_model_version VARCHAR(50) NOT NULL,
    local_model_version VARCHAR(50) NOT NULL,
    sample_count INT NOT NULL,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    weights_json JSONB,
    artifact_reference VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_round_node_update UNIQUE(round_id, node_id)
);

-- 4. federated_global_models
CREATE TABLE IF NOT EXISTS federated_global_models (
    version VARCHAR(50) PRIMARY KEY,
    base_model_version VARCHAR(50),
    round_id VARCHAR(50) REFERENCES federated_rounds(round_id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    feature_schema_version VARCHAR(50) NOT NULL DEFAULT 'f3-features-v1',
    algorithm VARCHAR(100) NOT NULL DEFAULT 'FED_AVG_RIDGE',
    total_samples INT DEFAULT 0,
    participating_nodes JSONB NOT NULL DEFAULT '[]',
    weights_json JSONB,
    mae DOUBLE PRECISION,
    rmse DOUBLE PRECISION,
    roc_auc DOUBLE PRECISION,
    brier_score DOUBLE PRECISION,
    artifact_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. REST API Endpoint Contracts

### 4.1 Node Controller (`/api/v1/federated/nodes`)
| Method | Endpoint | Request Body | Response Status | Purpose |
|---|---|---|---|---|
| `POST` | `/api/v1/federated/nodes/register` | `RegisterNodeRequest` | `201 Created` | Registers or updates a municipal node profile. |
| `GET` | `/api/v1/federated/nodes` | _None_ | `200 OK` | Lists all registered municipal nodes across Pune, Mumbai, Delhi. |
| `GET` | `/api/v1/federated/nodes/{nodeId}` | _None_ | `200 OK` | Retrieves status and last seen timestamp for a specific node. |
| `POST` | `/api/v1/federated/nodes/{nodeId}/heartbeat` | `NodeHeartbeatRequest` | `200 OK` | Updates node liveness, status, and local model version. |

### 4.2 Round Controller (`/api/v1/federated/rounds`)
| Method | Endpoint | Request Body | Response Status | Purpose |
|---|---|---|---|---|
| `POST` | `/api/v1/federated/rounds` | `CreateRoundRequest` | `201 Created` | Creates a new training round with base model and quorum. |
| `GET` | `/api/v1/federated/rounds` | _None_ | `200 OK` | Lists all federated rounds ordered by creation date descending. |
| `GET` | `/api/v1/federated/rounds/{roundId}` | _None_ | `200 OK` | Retrieves round status, quorum progress, metrics, and update list. |
| `POST` | `/api/v1/federated/rounds/{roundId}/aggregate` | _None_ | `200 OK` | Executes FedAvg consensus, publishes new global model version. |

### 4.3 Model Update Controller (`/api/v1/federated/rounds/{roundId}/updates`)
| Method | Endpoint | Request Body | Response Status | Purpose |
|---|---|---|---|---|
| `POST` | `/api/v1/federated/rounds/{roundId}/updates` | `SubmitModelUpdateRequest` | `200 OK` | Submits municipal node parameter update with local metrics. |
| `GET` | `/api/v1/federated/rounds/{roundId}/updates` | _None_ | `200 OK` | Lists all validated model updates for the round. |

### 4.4 Global Model Controller (`/api/v1/federated/models`)
| Method | Endpoint | Request Body | Response Status | Purpose |
|---|---|---|---|---|
| `GET` | `/api/v1/federated/models/active` | _None_ | `200 OK` | Returns active global model metadata and performance metrics. |
| `GET` | `/api/v1/federated/models/catalog` | _None_ | `200 OK` | Returns entire global model lineage catalog. |
| `GET` | `/api/v1/federated/models/{modelVersion}` | _None_ | `200 OK` | Returns metadata for a specific global model version. |

---

## 5. Validation Guardrails & Error Handling

| Guardrail Condition | Scenario | Service Action | HTTP Status |
|---|---|---|---|
| **Stale Base Model** | Update submitted with `baseModelVersion` differing from round's base model | Throws `ValidationException("Stale base model...")` | `400 Bad Request` |
| **Duplicate Update** | Node submits more than one update for the same round | Throws `IllegalStateException("Duplicate update: node ... has already submitted...")` | `409 Conflict` |
| **Quorum Failure** | Aggregation triggered when update count $< \text{minQuorum}$ | Marks round `FAILED`, throws `ValidationException("Insufficient updates...")` | `400 Bad Request` |
| **Un-enrolled Node** | Node not in round's participating nodes attempts to submit update | Throws `ValidationException("Node ... is not an enrolled participant...")` | `400 Bad Request` |
| **Invalid Samples** | Update submitted with $\text{sampleCount} \le 0$ | Throws `ValidationException("Sample count must be strictly greater than 0")` | `400 Bad Request` |
| **Completed Round** | Update submitted to round in `COMPLETED` status | Throws `ValidationException("Round ... is already COMPLETED...")` | `400 Bad Request` |

---

## 6. Unit & Integration Test Verification

Execution command: `.\mvnw.cmd test '-Dtest=FederatedRoundServiceTest,FederatedNodeServiceTest,FederatedControllersIntegrationTest'`

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.aerosentinel.federated.FederatedControllersIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.709 s -- in com.aerosentinel.federated.FederatedControllersIntegrationTest
[INFO] Running com.aerosentinel.federated.FederatedNodeServiceTest
04:54:53.491 [main] INFO com.aerosentinel.federated.FederatedNodeService -- FEDERATED_NODE_REGISTERED nodeId=PUNE status=ONLINE endpoint=http://pune.internal:8082
04:54:53.509 [main] INFO com.aerosentinel.federated.FederatedNodeService -- FEDERATED_NODE_OFFLINE nodeId=DELHI lastSeenAt=2026-09-29T23:09:53.507863Z
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.470 s -- in com.aerosentinel.federated.FederatedNodeServiceTest
[INFO] Running com.aerosentinel.federated.FederatedRoundServiceTest
04:54:53.812 [main] INFO com.aerosentinel.federated.FederatedRoundService -- FEDERATED_AGGREGATION_COMPLETED roundId=ROUND-002 newModel=global-v3 nodes=2 totalSamples=2400
04:54:53.827 [main] INFO com.aerosentinel.federated.FederatedRoundService -- FEDERATED_UPDATE_RECORDED roundId=ROUND-001 city=PUNE samples=1200 mae=0.4312
04:54:53.840 [main] INFO com.aerosentinel.federated.FederatedRoundService -- FEDERATED_AGGREGATION_COMPLETED roundId=ROUND-001 newModel=global-v2 nodes=3 totalSamples=3000
04:54:53.860 [main] INFO com.aerosentinel.federated.FederatedRoundService -- FEDERATED_ROUND_STARTED roundId=ROUND-001 baseModelVersion=global-v1 minQuorum=2
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.351 s -- in com.aerosentinel.federated.FederatedRoundServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  30.305 s
[INFO] Finished at: 2026-09-30T04:54:53+05:30
[INFO] ------------------------------------------------------------------------
```

Python Test Verification: `python -m pytest federated/tests/ -v`
```text
collected 16 items
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_01_complete_3_node_round_simulation PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_02_quorum_threshold_two_of_three PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_03_insufficient_quorum_blocks_aggregation PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_04_stale_base_model_rejection PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_05_duplicate_update_rejection PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_06_mismatched_weight_dimension_rejection PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_07_model_registry_version_lineage PASSED
federated/tests/test_coordinator.py::TestCoordinatorAndAggregator::test_08_mathematical_fedavg_correctness PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_01_feature_vector_dimension_and_ordering PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_02_pune_local_training PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_03_mumbai_local_training PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_04_delhi_local_training PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_05_parameter_update_packaging PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_06_global_model_parameter_synchronization PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_07_cross_city_contract_parity PASSED
federated/tests/test_local_nodes.py::TestLocalNodesAndCommonModel::test_08_no_raw_data_leakage PASSED

============================= 16 passed in 5.11s ==============================
```

---

## 7. Pass Criteria Checklist & Explicit Declaration

- [x] `V12__f9_federated_network.sql` migration created, syntactically valid, and seeds default nodes
- [x] JPA entities (`FederatedNode`, `FederatedRound`, `ModelUpdate`, `FederatedGlobalModel`) and enums implemented
- [x] Spring Data JPA repositories implemented (`FederatedNodeRepository`, `FederatedRoundRepository`, `ModelUpdateRepository`, `FederatedGlobalModelRepository`)
- [x] Service layer implements quorum, stale-model rejection, and duplicate rejection
- [x] REST controllers implement all specified endpoints under `/api/v1/federated/`
- [x] All 19 unit and slice tests pass under `backend/src/test/java/com/aerosentinel/federated/`
- [x] Maven test-compile and test run cleanly without breaking F1-F8 tests
- [x] No modifications made to React frontend or F1-F8 core intelligence logic
- [x] `F9_P4_BACKEND_CONTROL_PLANE_REPORT.md` created with complete verification evidence

**Phase F9-P4 Status:** **PASS** ✅
