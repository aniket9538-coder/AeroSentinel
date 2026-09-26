"""
AeroSentinel - Domain Cleaning Pipelines
Provides deterministic cleaning for Ground Air Quality and Surface Weather datasets.
"""
import json
from pathlib import Path
from typing import Dict, Optional, Tuple
import uuid
import numpy as np
import pandas as pd

from app.schemas.canonical import DataStatus, QualityFlag
from preprocessing.normalization import (
    WEATHER_RANGES,
    classify_pollutant_value,
    decompose_wind,
)
from preprocessing.spatial_alignment import bind_h3_cell, validate_coordinates
from preprocessing.temporal_alignment import align_to_hourly_bin, normalize_timestamp


def clean_air_quality_data(
    raw_df: pd.DataFrame,
    stations_geojson_path: Path,
    city_fallback: str = "pune"
) -> pd.DataFrame:
    """
    Cleans, georeferences, and validates air quality observation records.
    Maps CPCB/MPCB portal naming variations to canonical schema.
    """
    df = raw_df.copy()
    if df.empty:
        return pd.DataFrame()

    # 0. Canonical column mapping for CPCB / MPCB raw downloads
    cpcb_rename_map = {
        "Station ID": "station_id",
        "station id": "station_id",
        "Timestamp": "observed_at",
        "timestamp": "observed_at",
        "From Date": "observed_at",
        "To Date": "observed_at_end",
        "City": "city_id",
        "city": "city_id",
        "Station Name": "station_name",
        "PM2.5 (µg/m³)": "pm25",
        "PM2.5": "pm25",
        "pm2_5": "pm25",
        "PM10 (µg/m³)": "pm10",
        "PM10": "pm10",
        "NO2 (µg/m³)": "no2",
        "NO2": "no2",
        "SO2 (µg/m³)": "so2",
        "SO2": "so2",
        "CO (mg/m³)": "co",
        "CO": "co",
        "Ozone (µg/m³)": "o3",
        "Ozone": "o3",
        "NH3 (µg/m³)": "nh3",
    }
    df = df.rename(columns=cpcb_rename_map)

    # Standardize string fields
    if "station_id" not in df.columns:
        df["station_id"] = "site_katraj_dairy"
    else:
        df["station_id"] = df["station_id"].astype(str).str.strip()

    if "city_id" not in df.columns:
        df["city_id"] = city_fallback
    else:
        df["city_id"] = df["city_id"].astype(str).str.lower().str.strip()

    # Standardize timestamp reference
    if "observed_at" not in df.columns and "timestamp" in df.columns:
        df["observed_at"] = df["timestamp"]

    # 1. Load station metadata for spatial georeferencing
    station_map: Dict[str, dict] = {}
    if stations_geojson_path.exists():
        with open(stations_geojson_path, "r", encoding="utf-8") as f:
            stations_data = json.load(f)

        for feat in stations_data.get("features", []):
            props = feat.get("properties", {})
            sid = str(props.get("station_id", "")).strip()
            coords = feat.get("geometry", {}).get("coordinates", [None, None])
            if sid and len(coords) >= 2:
                station_map[sid] = {
                    "name": props.get("station_name") or props.get("name", sid),
                    "latitude": float(coords[1]),
                    "longitude": float(coords[0]),
                    "city_id": props.get("city_id", city_fallback)
                }

    # 2. Deterministic deduplication
    if "ingestion_timestamp" in df.columns:
        df = df.sort_values(by=["station_id", "observed_at", "ingestion_timestamp"], ascending=[True, True, True])
    df = df.drop_duplicates(subset=["station_id", "observed_at"], keep="last")

    cleaned_records = []

    # Default fallback coordinates for Katraj Dairy, Pune (MPCB Station)
    KATRAJ_LAT, KATRAJ_LON = 18.4529, 73.8553

    for _, row in df.iterrows():
        sid = str(row["station_id"]).strip()
        st_meta = station_map.get(sid)

        # Coordinate resolution
        if st_meta:
            lat = st_meta["latitude"]
            lon = st_meta["longitude"]
            sname = st_meta["name"]
            cid = st_meta["city_id"]
        else:
            lat = float(row.get("latitude")) if pd.notna(row.get("latitude")) else KATRAJ_LAT
            lon = float(row.get("longitude")) if pd.notna(row.get("longitude")) else KATRAJ_LON
            sname = str(row.get("station_name", "Katraj Dairy, Pune - MPCB"))
            cid = str(row.get("city_id", city_fallback))

        valid_coords, _ = validate_coordinates(lat, lon, bbox=None)
        if not valid_coords:
            continue

        h3_cell = bind_h3_cell(lat, lon, resolution=8, bbox=None)

        # Temporal validation
        norm_ts = normalize_timestamp(row["observed_at"])
        if norm_ts is None:
            continue

        # Flexible pollutant value parsing
        raw_pm25 = row.get("pm25") if pd.notna(row.get("pm25")) else row.get("pm2_5")

        # Pollutant validation
        pm25_flag, clean_pm25 = classify_pollutant_value(raw_pm25, "pm25")
        pm10_flag, clean_pm10 = classify_pollutant_value(row.get("pm10"), "pm10")
        no2_flag, clean_no2   = classify_pollutant_value(row.get("no2"), "no2")
        so2_flag, clean_so2   = classify_pollutant_value(row.get("so2"), "so2")

        # Overall row quality flag
        row_flags = [pm25_flag, pm10_flag, no2_flag, so2_flag]
        if QualityFlag.INVALID in row_flags or pm25_flag == QualityFlag.INVALID:
            final_quality = QualityFlag.INVALID
        elif QualityFlag.SUSPECT in row_flags:
            final_quality = QualityFlag.SUSPECT
        elif pm25_flag == QualityFlag.MISSING:
            final_quality = QualityFlag.MISSING
        else:
            final_quality = QualityFlag.VALID

        record = {
            "observation_id": str(uuid.uuid4()),
            "source": str(row.get("source", "CPCB")),
            "source_record_id": str(row.get("source_record_id", f"{sid}_{row['observed_at']}")),
            "observed_at": norm_ts,
            "station_id": sid,
            "station_name": sname,
            "city_id": cid,
            "latitude": lat,
            "longitude": lon,
            "h3_cell_id": h3_cell,
            "pm25": clean_pm25,
            "pm10": clean_pm10,
            "no2": clean_no2,
            "so2": clean_so2,
            "co": float(row.get("co")) if pd.notna(row.get("co")) else np.nan,
            "o3": float(row.get("o3")) if pd.notna(row.get("o3")) else np.nan,
            "quality_flag": final_quality.value,
            "data_status": DataStatus.HISTORICAL.value,
            "ingestion_timestamp": pd.Timestamp.now(pd.Timestamp("now", tz="UTC").tz)
        }
        cleaned_records.append(record)

    out_df = pd.DataFrame(cleaned_records)
    if out_df.empty:
        return out_df

    # Align to hourly binning
    out_df = align_to_hourly_bin(out_df, timestamp_col="observed_at", bin_col="hourly_bin")

    # Gap-limited forward filling per station (maximum 2 hours forward fill)
    out_df = out_df.sort_values(by=["station_id", "hourly_bin"])
    out_df["pm25_clean"] = out_df.groupby("station_id")["pm25"].transform(
        lambda s: s.ffill(limit=2)
    )

    return out_df


def clean_weather_data(
    raw_df: pd.DataFrame,
    city_centroid: Tuple[float, float] = (18.5204, 73.8567),
    city_id: str = "pune"
) -> pd.DataFrame:
    """
    Cleans, validates, and vectorizes surface meteorological observations.
    Maps Open-Meteo downloaded column names to canonical schema.
    """
    df = raw_df.copy()
    if df.empty:
        return pd.DataFrame()

    # Open-Meteo column renaming
    weather_rename_map = {
        "time": "observed_at",
        "date": "observed_at",
        "Timestamp": "observed_at",
        "temperature_2m (°C)": "temperature",
        "temperature_2m": "temperature",
        "relative_humidity_2m (%)": "humidity",
        "relativehumidity_2m": "humidity",
        "wind_speed_10m (km/h)": "wind_speed",
        "windspeed_10m": "wind_speed",
        "wind_direction_10m (°)": "wind_direction",
        "winddirection_10m": "wind_direction",
        "precipitation (mm)": "rainfall",
        "rain (mm)": "rainfall",
        "surface_pressure (hPa)": "pressure",
    }
    df = df.rename(columns=weather_rename_map)

    if "observed_at" not in df.columns and "timestamp" in df.columns:
        df["observed_at"] = df["timestamp"]

    df = df.drop_duplicates(subset=["observed_at"], keep="last")

    cleaned_records = []
    lat, lon = city_centroid
    h3_cell = bind_h3_cell(lat, lon, resolution=8, bbox=None)

    for _, row in df.iterrows():
        norm_ts = normalize_timestamp(row["observed_at"])
        if norm_ts is None:
            continue

        temp = float(row.get("temperature", np.nan))
        hum = float(row.get("humidity", np.nan))
        wspd = float(row.get("wind_speed", np.nan))
        wdir = float(row.get("wind_direction", np.nan))
        rain = float(row.get("rainfall", 0.0))
        pres = float(row.get("pressure", np.nan))

        is_valid = True
        if pd.notna(temp) and not (WEATHER_RANGES["temperature"]["min"] <= temp <= WEATHER_RANGES["temperature"]["max"]):
            is_valid = False
        if pd.notna(hum) and not (WEATHER_RANGES["humidity"]["min"] <= hum <= WEATHER_RANGES["humidity"]["max"]):
            is_valid = False
        if pd.notna(wspd) and not (WEATHER_RANGES["wind_speed"]["min"] <= wspd <= WEATHER_RANGES["wind_speed"]["max"]):
            is_valid = False
        if pd.notna(rain) and not (WEATHER_RANGES["rainfall"]["min"] <= rain <= WEATHER_RANGES["rainfall"]["max"]):
            is_valid = False

        quality = QualityFlag.VALID if is_valid else QualityFlag.INVALID
        if not is_valid:
            temp, hum, wspd, wdir, rain = np.nan, np.nan, np.nan, np.nan, np.nan

        u_wind, v_wind = decompose_wind(wspd, wdir)

        record = {
            "observation_id": str(uuid.uuid4()),
            "source": str(row.get("source", "IMD_OPENMETEO")),
            "observed_at": norm_ts,
            "city_id": str(row.get("city_id", city_id)).lower().strip(),
            "latitude": lat,
            "longitude": lon,
            "h3_cell_id": h3_cell,
            "temperature": temp,
            "humidity": hum,
            "wind_speed": wspd,
            "wind_direction": wdir,
            "wind_u": u_wind,
            "wind_v": v_wind,
            "rainfall": rain,
            "pressure": pres,
            "quality_flag": quality.value,
            "data_status": DataStatus.HISTORICAL.value,
            "ingestion_timestamp": pd.Timestamp.now(pd.Timestamp("now", tz="UTC").tz)
        }
        cleaned_records.append(record)

    out_df = pd.DataFrame(cleaned_records)
    if out_df.empty:
        return out_df

    out_df = align_to_hourly_bin(out_df, timestamp_col="observed_at", bin_col="hourly_bin")

    # Safe time-indexed interpolation for short meteorological gaps (up to 2 steps)
    out_df = out_df.sort_values(by="hourly_bin")
    interp_cols = ["temperature", "humidity", "wind_speed", "wind_u", "wind_v", "rainfall", "pressure"]
    existing_cols = [c for c in interp_cols if c in out_df.columns]

    out_df = out_df.set_index("hourly_bin")
    for col in existing_cols:
        out_df[col] = out_df[col].interpolate(method="time", limit=2)
    out_df = out_df.reset_index()

    return out_df