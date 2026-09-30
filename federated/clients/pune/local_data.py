"""
AeroSentinel - Pune Municipal Node Local Data Adapter
File: federated/clients/pune/local_data.py

Simulates isolated municipal environmental observations for Pune.
Strictly adheres to the 36-feature schema (f3-features-v1) and generates
characteristic Pune environmental distributions (traffic peaks + residential burning).
"""

from typing import Optional, List
import numpy as np
import pandas as pd

from federated.models.local_model import ORDERED_FEATURE_NAMES

PUNE_NODE_ID = "PUNE"
PUNE_CITY_ID = "550e8400-e29b-41d4-a716-446655440001"
DEFAULT_SAMPLE_COUNT = 1200


class PuneLocalDataLoader:
    """
    Local data loader and generator for the Pune Municipal Node.
    Generates synthetic municipal data reflecting Pune's urban microclimate and traffic patterns.
    """

    def __init__(self, random_seed: int = 42):
        self.node_id = PUNE_NODE_ID
        self.city_id = PUNE_CITY_ID
        self.random_seed = random_seed

    def get_dataset(self, sample_count: Optional[int] = None) -> pd.DataFrame:
        """
        Generates/returns DataFrame with exact 36 features + target_hotspot.
        """
        n = sample_count or DEFAULT_SAMPLE_COUNT
        rng = np.random.RandomState(self.random_seed)

        # Pune center: lat ~18.5204, lon ~73.8567
        lat = rng.normal(18.5204, 0.05, n)
        lon = rng.normal(73.8567, 0.05, n)

        # Pollutants (moderate background + urban traffic)
        pm10 = np.clip(rng.normal(78.0, 22.0, n), 15.0, 250.0)
        no2 = np.clip(rng.normal(44.0, 14.0, n), 10.0, 120.0)
        so2 = np.clip(rng.normal(18.0, 6.0, n), 2.0, 60.0)
        co = np.clip(rng.normal(1.15, 0.35, n), 0.2, 4.0)
        o3 = np.clip(rng.normal(32.0, 9.0, n), 5.0, 90.0)

        # Temporal features
        hour = rng.randint(0, 24, n)
        day_of_week = rng.randint(0, 7, n)
        is_weekend = (day_of_week >= 5).astype(float)
        hour_sin = np.sin(2 * np.pi * hour / 24.0)
        hour_cos = np.cos(2 * np.pi * hour / 24.0)
        dow_sin = np.sin(2 * np.pi * day_of_week / 7.0)
        dow_cos = np.cos(2 * np.pi * day_of_week / 7.0)

        # Meteorology (moderate plateau climate)
        temperature = np.clip(rng.normal(28.5, 4.0, n), 16.0, 42.0)
        humidity = np.clip(rng.normal(56.0, 12.0, n), 20.0, 95.0)
        wind_speed = np.clip(rng.normal(3.2, 1.1, n), 0.5, 9.0)
        wind_direction = rng.uniform(0.0, 360.0, n)
        wind_rad = np.deg2rad(wind_direction)
        wind_u = -wind_speed * np.sin(wind_rad)
        wind_v = -wind_speed * np.cos(wind_rad)
        rainfall = np.where(rng.uniform(0, 1, n) < 0.15, rng.exponential(3.0, n), 0.0)
        pressure = rng.normal(1008.0, 4.0, n)

        # Spatial context & station network
        pm25_spatial_lag = np.clip(rng.normal(46.0, 14.0, n), 10.0, 180.0)
        nearest_station_dist = np.clip(rng.exponential(2.8, n), 0.4, 14.0)
        stations_within_5km = np.where(nearest_station_dist <= 5.0, rng.choice([1, 2, 3], n), 0)
        coverage_gap = (nearest_station_dist > 7.0).astype(float)

        # Land use & receptors
        dist_industrial = np.clip(rng.exponential(4.2, n), 0.3, 18.0)
        dist_major_road = np.clip(rng.exponential(0.9, n), 0.05, 5.0)
        sensitive_receptors = rng.choice([0, 1, 2, 3, 4], n, p=[0.2, 0.35, 0.25, 0.15, 0.05])
        industrial_within_2km = (dist_industrial <= 2.0).astype(float)

        # Fire indicators (localized biomass / agricultural waste burning)
        fire_count = rng.choice([0, 1, 2, 3], n, p=[0.60, 0.25, 0.10, 0.05])
        fire_frp_sum = np.where(fire_count > 0, fire_count * rng.uniform(10.0, 35.0, n), 0.0)
        fire_frp_mean = np.divide(fire_frp_sum, fire_count, out=np.zeros_like(fire_frp_sum), where=fire_count > 0)
        nearest_fire_dist = np.where(fire_count > 0, rng.uniform(3.0, 22.0, n), 25.0)
        fire_decay = np.where(fire_count > 0, fire_frp_mean / (nearest_fire_dist + 1.0), 0.0)
        fire_upwind_score = np.where(fire_count > 0, rng.uniform(0.1, 0.7, n), 0.0)

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

        # Ensure exact 36-column ordering
        df = df[ORDERED_FEATURE_NAMES]

        # Target generation: risk exceedance rule for Pune (traffic + pm10 + evening lag)
        logits = (
            0.035 * df["pm10"]
            + 0.025 * df["no2"]
            + 0.030 * df["pm25_spatial_lag_mean"]
            - 0.15 * df["wind_speed"]
            - 0.08 * df["dist_to_major_road" if "dist_to_major_road" in df else "dist_to_nearest_major_road_km"]
            + 0.40 * df["industrial_zone_within_2km_flag"]
            + 0.50 * df["fire_upwind_alignment_score"]
            - 4.2
        )
        probs = 1.0 / (1.0 + np.exp(-np.clip(logits, -10.0, 10.0)))
        df["target_hotspot"] = (rng.uniform(0, 1, n) < probs).astype(int)

        return df
