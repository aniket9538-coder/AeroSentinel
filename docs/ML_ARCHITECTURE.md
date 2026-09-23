# AeroSentinel — Machine Learning Architecture

---

## 1. Machine Learning Strategy

AeroSentinel deliberately decouples numerical modeling from qualitative and natural language synthesis:

1. **Hotspot Anomaly & Risk Detection**:
   - Model: Gradient Boosted Decision Trees (XGBoost) and Isolation Forests for unsupervised anomaly detection.
   - Objective: Output probability $P(\text{Spike} \mid \text{Grid Features})$ and assign risk category (`LOW`, `MEDIUM`, `HIGH`).
2. **Short-Term PM2.5 Forecast (1–6 Hour Horizon)**:
   - Model: Autoregressive Gradient Boosted Trees (XGBoost / LightGBM) trained on temporal lags, meteorological variables, and upstream wind vectors.
   - Objective: Predict rolling hourly concentrations $\hat{y}_{t+1}, \dots, \hat{y}_{t+6}$ alongside standard deviation uncertainty bounds.
3. **Google Gemini Multimodal Synthesis**:
   - Gemini Vision analyzes citizen incident photos to corroborate smoke, fire, or dust.
   - Gemini GenAI generates structured evidence narratives explaining *why* an H3 cell was flagged.

---

## 2. Feature Engineering Pipeline

For each H3 cell $i$ at timestamp $t$:
- **Temporal Lags**: $PM_{2.5}(t-1\text{h}), PM_{2.5}(t-3\text{h}), PM_{2.5}(t-6\text{h})$, and rate of change $\Delta PM = PM(t) - PM(t-1\text{h})$.
- **Meteorology**: Temperature, relative humidity, wind speed ($v$), and wind direction $(\theta)$ decomposed into Cartesian components:
  $$u = -v \cdot \sin(\theta), \quad w = -v \cdot \cos(\theta)$$
- **Thermal Fire Inversion**: Count of active FIRMS fire detections within radius $R$ ($5\text{ km}$), minimum distance to fire, and upwind alignment:
  $$\text{FireAlignment} = \cos(\theta_{\text{wind}} - \theta_{\text{fire}})$$
- **Satellite Tropospheric Indicator**: Normalized Sentinel-5P NO2 column density.
- **Citizen Feedback Velocity**: Count of verified citizen reports submitted within the cell in the prior 3 hours.

---

## 3. Evaluation Metrics

- **Forecast Accuracy**:
  - Mean Absolute Error (MAE): $\frac{1}{N}\sum |y_i - \hat{y}_i|$
  - Root Mean Squared Error (RMSE): $\sqrt{\frac{1}{N}\sum (y_i - \hat{y}_i)^2}$
  - Symmetric Mean Absolute Percentage Error (sMAPE)
- **Hotspot Detection**:
  - Precision, Recall, and F1-Score on high-pollution spike events ($PM_{2.5} > 150\,\mu\text{g/m}^3$).
  - Lead time: Average advance warning provided before a sensor measures an official peak.
