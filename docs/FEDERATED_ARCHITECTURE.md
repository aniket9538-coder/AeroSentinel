# AeroSentinel — Multi-City Federated Learning Architecture

---

## 1. Prototype Overview & Motivation

Different Indian municipal regions possess distinct pollution profiles:
- **Delhi NCR**: Dominated by seasonal agricultural stubble burning, regional winter temperature inversions, and brick kiln emissions.
- **Mumbai**: Coastal meteorology, maritime moisture, sea breeze circulations, and dense coastal traffic corridors.
- **Pune**: Complex valley basin topography, industrial MIDC clusters, and rapid urban construction dust.

Municipal pollution boards frequently resist centralizing raw, unprocessed sensory logs due to municipal data sovereignty regulations and network bandwidth constraints. **Federated Learning (FL)** enables multi-city collaborative model training where:
1. Each city node trains a local model on its private data partitions.
2. Only model parameter updates (gradient deltas or tree split weights) are transmitted to the coordinator.
3. The central coordinator aggregates parameter updates into a shared global model without observing raw local datasets.

---

## 2. Federated Coordination Workflow

```text
                  Central Coordinator
             (Round Management & FedAvg)
                           │
         ┌─────────────────┼─────────────────┐
         │ Global Weights  │ Global Weights  │ Global Weights
         ▼                 ▼                 ▼
     Pune Node        Mumbai Node       Delhi Node
   (Local Model)     (Local Model)     (Local Model)
         │                 │                 │
   Train on Local    Train on Local    Train on Local
     City Data         City Data         City Data
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │ Parameter Deltas
                           ▼
                  Federated Aggregator
             (Compute Weighted Average)
                           │
                           ▼
                  Updated Global Model
                     (Version N+1)
```

---

## 3. Privacy & Extension Points (`federated/privacy/`)

In the hackathon demonstration:
- City nodes run as separate simulated workers in `federated/clients/` (Pune, Mumbai, Delhi).
- The aggregator calculates sample-weighted Federated Averaging:
  $$w^{t+1} = \sum_{k=1}^K \frac{n_k}{N} w_k^{t+1}$$
- Extension modules (`differential_privacy.py`, `secure_aggregation.py`) provide interfaces for adding Gaussian noise mechanisms and cryptographic masking for production deployments.
