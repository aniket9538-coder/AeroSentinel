"""
AeroSentinel - Mumbai Coastal Node Local Data Adapter
File: federated/clients/mumbai/local_data.py

Simulates isolated municipal environmental observations for Mumbai.
Strictly adheres to the 36-feature schema (f3-features-v1) and generates
characteristic Mumbai coastal distributions (high humidity + maritime wind dispersion).
"""

from typing import Optional
import numpy as np
import pandas as pd

from federated.models.local_model import ORDERED_FEATURE_NAMES

MUMBAI_NODE_ID = "MUMBAI"
MUMBAI_CITY_ID = "550e8400-e29b-41d4-a716-446655440002"
DEFAULT_SAMPLE_COUNT = 800


class MumbaiLocalDataLoader:
    """
    Local data loader and generator for the Mumbai Coastal Node.
    Generates synthetic municipal data reflecting Mumbai's maritime boundary layer and coastal winds.
    """

    def __init__(self, random_seed: int = 101):
        self.node_id = MUMBAI_NODE_ID
        self.city_id = MUMBAI_CITY_ID
        self.random_seed = random_seed

    def get_dataset(self, sample_count: Optional[int] = None) -> pd.DataFrame:
        """
        Generates/returns DataFrame with exact 36 features + target_hotspot.
        """
        n = sample_count or DEFAULT_SAMPLE_COUNT
        rng = np.random.RandomState(self.random_seed)

        # Mumbai center: lat ~19.0760, lon ~72.8777
        lat = rng.normal(19.0760, 0.06, n)
        lon = rng.normal(72.8777, 0.04, n)

        # Pollutants (moderate coastal baseline)
        pm10 = np.clip(rng.normal(68.0, 20.0, n), 12.0, 220.0)
        no2 = np.clip(rng.normal(48.0, 15.0, n), 12.0, 130.0)
        so2 = np.clip(rng.normal(22.0, 8.0, n), 3.0, 75.0)
        co = np.clip(rng.normal(1.25, 0.40, n), 0.3, 4.5)
        o3 = np.clip(rng.normal(28.0, 8.0, n), 4.0, 80.0)

        # Temporal features
        hour = rng.randint(0, 24, n)
        day_of_week = rng.randint(0, 7, n)
        is_weekend = (day_of_week >= 5).astype(float)
        hour_sin = np.sin(2 * np.pi * hour / 24.0)
        hour_cos = np.cos(2 * np.pi * hour / 24.0)
        dow_sin = np.sin(2 * np.pi * day_of_week / 7.0)
        dow_cos = np.cos(2 * np.pi * day_of_week / 7.0)

        # Meteorology (coastal marine climate: high humidity, stronger winds)
        temperature = np.clip(rng.normal(31.0, 2.8, n), 22.0, 39.0)
        humidity = np.clip(rng.normal(82.0, 8.0, n), 55.0, 99.0)  # High marine humidity
        wind_speed = np.clip(rng.normal(5.4, 1.8, n), 1.0, 14.0)  # Stronger sea breeze
        wind_direction = rng.normal(250.0, 45.0, n) % 360.0  # Prevailing westerly/southwesterly sea breeze
        wind_rad = np.deg2rad(wind_direction)
        wind_u = -wind_speed * np.sin(wind_rad)
        wind_v = -wind_speed * np.cos(wind_rad)
        rainfall = np.where(rng.uniform(0, 1, n) < 0.28, rng.exponential(6.0, n), 0.0)
        pressure = rng.normal(1012.0, 3.5, n)

        # Spatial context & station network
        pm25_spatial_lag = np.clip(rng.normal(42.0, 12.0, n), 10.0, 150.0)
        nearest_station_dist = np.clip(rng.exponential(2.4, n), 0.3, 11.0)
        stations_within_5km = np.where(nearest_station_dist <= 5.0, rng.choice([1, 2, 3, 4], n), 0)
        coverage_gap = (nearest_station_dist > 7.0).astype(float)

        # Land use & receptors (dense urban island corridor)
        dist_industrial = np.clip(rng.exponential(3.8, n), 0.2, 15.0)
        dist_major_road = np.clip(rng.exponential(0.6, n), 0.02, 3.5)
        sensitive_receptors = rng.choice([1, 2, 3, 4, 5], n, p=[0.15, 0.30, 0.30, 0.15, 0.10])
        industrial_within_2km = (dist_industrial <= 2.0).astype(float)

        # Fire indicators (very low in dense coastal metropolitan area)
        fire_count = rng.choice([0, 1, 2], n, p=[0.85, 0.12, 0.03])
        fire_frp_sum = np.where(fire_count > 0, fire_count * rng.uniform(8.0, 25.0, n), 0.0)
        fire_frp_mean = np.divide(fire_frp_sum, fire_count, out=np.zeros_like(fire_frp_sum), where=fire_count > 0)
        nearest_fire_dist = np.where(fire_count > 0, rng.uniform(5.0, 24.0, n), 25.0)
        fire_decay = np.where(fire_count > 0, fire_frp_mean / (nearest_fire_dist + 1.0), 0.0)
        fire_upwind_score = np.where(fire_count > 0, rng.uniform(0.05, 0.4, n), 0.0)

        data = {
            "latitude": lat,
            "longitude": lon,
            "pm10": pm10,
            "no2": no2,
            "so2": so2,
            "co": co,
            "o3": o3,
            "hour": hour.astype(float),
            "day_of_week": day_of_week.astype(float),
            "is_weekend": is_weekend,
            "hour_sin": hour_sin,
            "hour_cos": hour_cos,
            "dow_sin": dow_sin,
            "dow_cos": dow_cos,
            "temperature": temperature,
            "humidity": humidity,
            "wind_speed": wind_speed,
            "wind_direction": wind_direction,
            "wind_u": wind_u,
            "wind_v": wind_v,
            "rainfall": rainfall,
            "pressure": pressure,
            "pm25_spatial_lag_mean": pm25_spatial_lag,
            "nearest_station_distance_km": nearest_station_dist,
            "stations_within_5km_count": stations_within_5km.astype(float),
            "monitoring_coverage_gap_flag": coverage_gap,
            "dist_to_nearest_industrial_km": dist_industrial,
            "dist_to_nearest_major_road_km": dist_major_road,
            "sensitive_receptors_count_2km": sensitive_receptors.astype(float),
            "industrial_zone_within_2km_flag": industrial_within_2km,
            "fire_count_24h_25km": fire_count.astype(float),
            "fire_frp_sum_24h_25km": fire_frp_sum,
            "fire_frp_mean_24h_25km": fire_frp_mean,
            "nearest_fire_distance_km": nearest_fire_dist,
            "fire_frp_distance_decay": fire_decay,
            "fire_upwind_alignment_score": fire_upwind_score,
        }

        df = pd.DataFrame(data)
        df = df[ORDERED_FEATURE_NAMES]

        # Target generation: Mumbai coastal stagnation & humidity entrapment
        logits = (
            0.030 * df["pm10"]
            + 0.035 * df["no2"]
            + 0.020 * df["so2"]
            + 0.025 * df["humidity"]
            - 0.22 * df["wind_speed"]
            + 0.45 * df["industrial_zone_within_2km_flag"]
            - 5.0
        )
        probs = 1.0 / (1.0 + np.exp(-np.clip(logits, -10.0, 10.0)))
        df["target_hotspot"] = (rng.uniform(0, 1, n) < probs).astype(int)

        return df
