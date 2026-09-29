# AeroSentinel — Machine Learning Architecture

---

## 1. Machine Learning Strategy

AeroSentinel deliberately decouples numerical modeling from qualitative and natural language synthesis:

1. **Hotspot Anomaly & Risk Detection**:
   - Model: Gradient Boosted Decision Trees (XGBoost) and Isolation Forests for unsupervised anomaly detection.
   - Objective: Output probability $P(\text{Spike} \mid \text{Grid Features})$ and assign risk category (`LOW`, `MEDIUM`, `HIGH`).
2. **Short-Term PM2.5 Forecast (Discrete Horizons: $T+1\text{h}, T+3\text{h}, T+6\text{h}$)**:
   - *(Note: Conceptual continuous 1–6h Autoregressive XGBoost is SUPERSEDED by the verified physical artifact `forecast_regressors_v1.joblib`; see [docs/F4_FORECAST_SPECIFICATION_V1.md](F4_FORECAST_SPECIFICATION_V1.md)).*
   - Model: Multi-Horizon `RandomForestRegressor` (100 estimators, max depth 14) independent per horizon ($1\text{h}, 3\text{h}, 6\text{h}$) fitted on forward-shifted PM2.5 targets.
   - Objective: Predict discrete future concentrations at $T+1\text{h}$, $T+3\text{h}$, and $T+6\text{h}$ alongside empirical residual quantiles ($P10$ to $P90$) with non-negative physical clamping ($\ge 0.0\,\mu\text{g/m}^3$).
3. **Google Gemini Multimodal Synthesis**:
   - Gemini Vision analyzes citizen incident photos to corroborate smoke, fire, or dust.
   - Gemini GenAI generates structured evidence narratives explaining *why* an H3 cell was flagged.

---

## 2. Feature Engineering Pipeline

For each H3 cell $i$ at timestamp $t$:
- **Core 36-Feature Vector (`f3-features-v1`)**: The verified model consumes an ordered vector of 36 numeric features.  
  *(Note: Conceptual lag variables $PM_{2.5}(t-1\text{h}), PM_{2.5}(t-3\text{h})$, etc., are SUPERSEDED by the 36-feature schema in `forecast_regressors_v1.joblib.feature_cols`, which uses `pm25_spatial_lag_mean` and co-pollutants while excluding raw $PM_{2.5}$ to prevent target leakage).*
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
