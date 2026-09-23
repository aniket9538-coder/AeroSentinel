# AeroSentinel — Hackathon Live Demonstration Script

---

## 1. Scene 1: The Monitoring Blindspot (1 Minute)
- **Visual**: Open the Public Dashboard (`/map`). Select city "Pune".
- **Narrative**: Show fixed CAAQMS stations as blue markers. Note that large gaps exist between stations.
- **Action**: Toggle the **H3 Risk Layer**. Highlight an orange/red hexagonal cell located 4 km from the nearest station.
- **Key Takeaway**: "AeroSentinel identifies pollution risks in the blind spots between official stations."

---

## 2. Scene 2: Multi-Source Evidence & Gemini Explanation (1.5 Minutes)
- **Visual**: Switch to Analyst View (`/analyst/hotspots`). Click on the high-risk H3 cell.
- **Narrative**: Examine the Evidence Panel showing:
  - PM2.5 rising trend (+28% over 3 hours)
  - Low wind speed (1.1 m/s stagnation)
  - NASA FIRMS fire detection 1.4 km upwind
  - Corroborating citizen report with uploaded photo
- **Action**: Click **"Generate Gemini AI Brief"**.
- **Result**: Gemini synthesizes the physical factors into a clear incident brief explaining *why* the cell is at high risk and confirming smoke detection from the citizen photo.

---

## 3. Scene 3: Authority Action & Field Triage (1.5 Minutes)
- **Visual**: Switch to Authority Dashboard (`/authority/incidents`).
- **Action**:
  - The high-risk cell has automatically generated an official Alert (Severity: `WARNING`).
  - Authority acknowledges the alert and clicks **"Schedule Field Inspection"**.
  - Assigns Inspection Team Beta with a mobile sensor.
  - Logs Action: "Dispatched municipal water misting cannon to mitigate localized dust".
  - Alert transitions to `RESOLVED`.
- **Key Takeaway**: "A complete loop from satellite/sensor signals to municipal action."

---

## 4. Scene 4: Multi-City Federated Prototype (1 Minute)
- **Visual**: Navigate to Federated Network (`/federated`).
- **Narrative**: Show Pune, Mumbai, and Delhi city nodes.
- **Action**: Trigger a federated aggregation round. Show local loss convergence, parameter transfer without sharing raw citizen data, and global model version incrementing to `v1.3`.
