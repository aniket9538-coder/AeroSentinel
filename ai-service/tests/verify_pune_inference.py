import joblib
import json
import numpy as np
import pandas as pd
from pathlib import Path

def main():
    artifact_path = Path("ai-service/models/artifacts/hotspot_classifier_v1.joblib")
    if not artifact_path.exists():
        print(f"ERROR: Artifact not found at {artifact_path}")
        return

    artifact = joblib.load(artifact_path)
    model = artifact["model"]
    cols = artifact["feature_cols"]
    threshold = artifact.get("operational_threshold", 0.20)

    # Real persisted Pune snapshot 1624baa3-a5f8-407b-b1c2-36bcee7650b1 (Shivajinagar, H3: 88608850e5fffff)
    raw_json = {
        "co": 0.9,
        "o3": 24.0,
        "no2": 37.0,
        "so2": 14.0,
        "hour": 18,
        "pm10": 120.0,
        "wind_u": 4.3604,
        "wind_v": 0.0761,
        "dow_cos": -0.2225,
        "dow_sin": -0.9749,
        "hour_cos": 0.0,
        "hour_sin": -1.0,
        "humidity": 77.0,
        "latitude": 18.5315,
        "pressure": 947.5,
        "rainfall": 0.0,
        "longitude": 73.8471,
        "is_weekend": 1,
        "wind_speed": 15.7,  # in km/h
        "day_of_week": 5,
        "temperature": 24.9,
        "wind_direction": 269.0,
        "fire_count_24h_25km": 0,
        "fire_frp_sum_24h_25km": 0.0,
        "pm25_spatial_lag_mean": 78.0,
        "fire_frp_mean_24h_25km": 0.0,
        "fire_frp_distance_decay": 0.0,
        "nearest_fire_distance_km": 50.0,
        "stations_within_5km_count": 2,
        "fire_upwind_alignment_score": 0.0,
        "nearest_station_distance_km": 0.27,
        "monitoring_coverage_gap_flag": 0,
        "dist_to_nearest_industrial_km": 3.5,
        "dist_to_nearest_major_road_km": 0.4,
        "sensitive_receptors_count_2km": 4,
        "industrial_zone_within_2km_flag": 0
    }

    # Wind speed normalization: 15.7 km/h / 3.6 = 4.36 m/s
    norm_json = dict(raw_json)
    norm_json["wind_speed"] = round(raw_json["wind_speed"] / 3.6, 2)

    df = pd.DataFrame([norm_json])[cols]

    proba = model.predict_proba(df)[0]
    pos_idx = int(np.where(model.classes_ == 1)[0][0])
    calibrated_prob = float(proba[pos_idx])
    is_hotspot = calibrated_prob >= threshold

    print(f"Artifact Algorithm: {artifact.get('algorithm')}")
    print(f"Classes: {model.classes_}")
    print(f"Positive class index: {pos_idx}")
    print(f"Feature count: {len(cols)}")
    print(f"Input shape: {df.shape}")
    print(f"Wind speed (raw km/h): {raw_json['wind_speed']} -> normalized (m/s): {norm_json['wind_speed']}")
    print(f"Calibrated probability: {calibrated_prob:.4f}")
    print(f"Operational threshold: {threshold:.2f}")
    print(f"isHotspot: {is_hotspot}")

    # Risk level mapping:
    if calibrated_prob < 0.20:
        risk_level = "LOW"
    elif calibrated_prob < 0.40:
        risk_level = "MODERATE"
    elif calibrated_prob < 0.70:
        risk_level = "HIGH"
    else:
        risk_level = "CRITICAL"
    print(f"Mapped Risk Level: {risk_level}")

if __name__ == "__main__":
    main()
