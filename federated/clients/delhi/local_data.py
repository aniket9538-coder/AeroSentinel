"""
AeroSentinel - Delhi Regional Node Local Data Adapter
File: federated/clients/delhi/local_data.py

Simulates isolated municipal environmental observations for Delhi.
Strictly adheres to the 36-feature schema (f3-features-v1) and generates
characteristic Delhi environmental distributions (severe seasonal PM2.5 + upwind fire smoke).
"""

from typing import Optional
import numpy as np
import pandas as pd

from federated.models.local_model import ORDERED_FEATURE_NAMES

DELHI_NODE_ID = "DELHI"
DELHI_CITY_ID = "550e8400-e29b-41d4-a716-446655440003"
DEFAULT_SAMPLE_COUNT = 1000


class DelhiLocalDataLoader:
    """
    Local data loader and generator for the Delhi Regional Node.
    Generates synthetic municipal data reflecting Delhi's severe winter particulate inversions and fire plumes.
    """

    def __init__(self, random_seed: int = 202):
        self.node_id = DELHI_NODE_ID
        self.city_id = DELHI_CITY_ID
        self.random_seed = random_seed

    def get_dataset(self, sample_count: Optional[int] = None) -> pd.DataFrame:
        """
        Generates/returns DataFrame with exact 36 features + target_hotspot.
        """
        n = sample_count or DEFAULT_SAMPLE_COUNT
        rng = np.random.RandomState(self.random_seed)

        # Delhi center: lat ~28.6139, lon ~77.2090
        lat = rng.normal(28.6139, 0.07, n)
        lon = rng.normal(77.2090, 0.07, n)

        # Pollutants (severe northern inland baseline)
        pm10 = np.clip(rng.normal(165.0, 55.0, n), 35.0, 480.0)
        no2 = np.clip(rng.normal(68.0, 22.0, n), 15.0, 180.0)
        so2 = np.clip(rng.normal(26.0, 10.0, n), 4.0, 95.0)
        co = np.clip(rng.normal(2.10, 0.75, n), 0.4, 7.5)
        o3 = np.clip(rng.normal(42.0, 14.0, n), 6.0, 110.0)

        # Temporal features
        hour = rng.randint(0, 24, n)
        day_of_week = rng.randint(0, 7, n)
        is_weekend = (day_of_week >= 5).astype(float)
        hour_sin = np.sin(2 * np.pi * hour / 24.0)
        hour_cos = np.cos(2 * np.pi * hour / 24.0)
        dow_sin = np.sin(2 * np.pi * day_of_week / 7.0)
        dow_cos = np.cos(2 * np.pi * day_of_week / 7.0)

        # Meteorology (inland continental climate: stagnant winds, thermal inversions)
        temperature = np.clip(rng.normal(22.0, 7.5, n), 6.0, 44.0)
        humidity = np.clip(rng.normal(64.0, 16.0, n), 18.0, 96.0)
        wind_speed = np.clip(rng.normal(1.9, 0.8, n), 0.2, 5.5)  # Stagnant low wind speed
        wind_direction = rng.normal(305.0, 35.0, n) % 360.0  # Prevailing northwesterly winds
        wind_rad = np.deg2rad(wind_direction)
        wind_u = -wind_speed * np.sin(wind_rad)
        wind_v = -wind_speed * np.cos(wind_rad)
        rainfall = np.where(rng.uniform(0, 1, n) < 0.08, rng.exponential(2.5, n), 0.0)
        pressure = rng.normal(1015.0, 5.0, n)

        # Spatial context & station network
        pm25_spatial_lag = np.clip(rng.normal(95.0, 35.0, n), 20.0, 320.0)
        nearest_station_dist = np.clip(rng.exponential(2.2, n), 0.2, 12.0)
        stations_within_5km = np.where(nearest_station_dist <= 5.0, rng.choice([1, 2, 3, 4, 5], n), 0)
        coverage_gap = (nearest_station_dist > 7.0).astype(float)

        # Land use & industrial ring
        dist_industrial = np.clip(rng.exponential(3.2, n), 0.1, 14.0)
        dist_major_road = np.clip(rng.exponential(0.7, n), 0.02, 4.0)
        sensitive_receptors = rng.choice([1, 2, 3, 4], n, p=[0.20, 0.40, 0.25, 0.15])
        industrial_within_2km = (dist_industrial <= 2.0).astype(float)

        # Fire indicators (heavy agricultural stubble / regional biomass burning)
        fire_count = rng.choice([0, 1, 2, 3, 4, 5, 6], n, p=[0.15, 0.25, 0.25, 0.15, 0.10, 0.06, 0.04])
        fire_frp_sum = np.where(fire_count > 0, fire_count * rng.uniform(25.0, 75.0, n), 0.0)
        fire_frp_mean = np.divide(fire_frp_sum, fire_count, out=np.zeros_like(fire_frp_sum), where=fire_count > 0)
        nearest_fire_dist = np.where(fire_count > 0, rng.uniform(2.0, 18.0, n), 25.0)
        fire_decay = np.where(fire_count > 0, fire_frp_mean / (nearest_fire_dist + 1.0), 0.0)
        fire_upwind_score = np.where(fire_count > 0, rng.uniform(0.45, 0.95, n), 0.0)  # Strong northwest alignment

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

        # Target generation: Delhi severe winter smog rule (driven by pm10, fires, stagnation)
        logits = (
            0.028 * df["pm10"]
            + 0.020 * df["no2"]
            + 0.025 * df["pm25_spatial_lag_mean"]
            - 0.25 * df["wind_speed"]
            + 0.85 * df["fire_upwind_alignment_score"]
            + 0.04 * df["fire_frp_sum_24h_25km"]
            + 0.35 * df["industrial_zone_within_2km_flag"]
            - 5.5
        )
        probs = 1.0 / (1.0 + np.exp(-np.clip(logits, -10.0, 10.0)))
        df["target_hotspot"] = (rng.uniform(0, 1, n) < probs).astype(int)

        return df
