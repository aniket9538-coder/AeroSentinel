"""
AeroSentinel - AI & ML Inference API Service
File: ai-service/app/main.py

Provides real-time REST endpoints for:
  - F3 Potential Hotspot Classification & Probability Calibration
  - F3 Multi-Horizon PM2.5 Forecasting (T+1, T+3, T+6)
  - Epistemic Uncertainty & Interpretability Signals
"""

from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any, Dict, List, Optional
import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

import sys
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from ml.inference.confidence import compute_prediction_confidence

# ---------------------------------------------------------
# Global Artifact Storage
# ---------------------------------------------------------
ml_artifacts: Dict[str, Any] = {}

ARTIFACTS_DIR = BASE_DIR / "models" / "artifacts"


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Loads serialized ML models, scalers, and calibration curves on startup.
    """
    hotspot_path = ARTIFACTS_DIR / "hotspot_classifier_v1.joblib"
    forecast_path = ARTIFACTS_DIR / "forecast_regressors_v1.joblib"

    if hotspot_path.exists():
        ml_artifacts["hotspot"] = joblib.load(hotspot_path)
        print(f"[INFO] Loaded Hotspot Classifier artifact: {hotspot_path.name}")
    else:
        print(f"[WARNING] Hotspot model artifact not found at {hotspot_path}")

    if forecast_path.exists():
        ml_artifacts["forecast"] = joblib.load(forecast_path)
        print(f"[INFO] Loaded Multi-Horizon Forecast artifact: {forecast_path.name}")
    else:
        print(f"[WARNING] Forecast model artifact not found at {forecast_path}")

    yield

    ml_artifacts.clear()


# ---------------------------------------------------------
# FastAPI Application Declaration
# ---------------------------------------------------------
app = FastAPI(
    title="AeroSentinel AI Inference Engine",
    description="Real-time multi-hazard hotspot detection and multi-horizon air quality forecasting.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------
# Request / Response Schemas
# ---------------------------------------------------------
class ObservationPayload(BaseModel):
    station_id: str = Field(default="site_5766", description="Station identifier")
    temperature: float = Field(default=25.0, description="Surface temperature in Celsius")
    humidity: float = Field(default=60.0, description="Relative humidity percentage")
    wind_speed: float = Field(default=3.5, description="Wind speed in km/h")
    wind_direction: float = Field(default=180.0, description="Wind direction in degrees")
    wind_u: Optional[float] = Field(default=0.0)
    wind_v: Optional[float] = Field(default=0.0)
    rainfall: Optional[float] = Field(default=0.0)
    pressure: Optional[float] = Field(default=950.0)
    pm25: Optional[float] = Field(default=45.0, description="Current PM2.5 in ug/m3")
    pm10: Optional[float] = Field(default=85.0)
    no2: Optional[float] = Field(default=20.0)
    so2: Optional[float] = Field(default=12.0)
    fire_count_24h_25km: Optional[int] = Field(default=0)
    fire_frp_sum_24h_25km: Optional[float] = Field(default=0.0)
    nearest_fire_distance_km: Optional[float] = Field(default=50.0)
    fire_upwind_alignment_score: Optional[float] = Field(default=0.0)
    hour_sin: Optional[float] = Field(default=0.0)
    hour_cos: Optional[float] = Field(default=1.0)
    dow_sin: Optional[float] = Field(default=0.0)
    dow_cos: Optional[float] = Field(default=1.0)
    is_weekend: Optional[int] = Field(default=0)


class HotspotResponse(BaseModel):
    is_hotspot: bool
    probability: float
    confidence_level: str
    epistemic_uncertainty_flag: int
    model_version: str


class ForecastResponse(BaseModel):
    station_id: str
    forecast_pm25: Dict[str, float]
    advisory: str


class HotspotInferenceRequest(BaseModel):
    h3Index: Optional[str] = Field(default=None)
    h3_index: Optional[str] = Field(default=None)
    cityName: Optional[str] = Field(default="Pune")
    city_name: Optional[str] = Field(default="Pune")
    cityId: Optional[str] = Field(default=None)
    city_id: Optional[str] = Field(default=None)
    featureSchemaVersion: Optional[str] = Field(default="f3-features-v1")
    features: Optional[List[float]] = Field(default=None, description="Exactly 36 ordered feature values")
    featureMap: Optional[Dict[str, float]] = Field(default=None, description="Key-value mapping of 36 feature names")
    feature_map: Optional[Dict[str, float]] = Field(default=None)


class HotspotInferenceResponse(BaseModel):
    h3Index: str
    riskScore: float
    riskLevel: str
    confidence: float
    isHotspot: bool
    operationalThreshold: float
    modelVersion: str
    engineType: str
    status: str
    message: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


# ---------------------------------------------------------
# Endpoints
# ---------------------------------------------------------
@app.get("/", tags=["System"])
def root():
    return {
        "service": "AeroSentinel AI Inference Engine",
        "status": "operational",
        "loaded_models": list(ml_artifacts.keys()),
    }


@app.get("/health", tags=["System"])
def health():
    return {
        "status": "healthy",
        "models_ready": len(ml_artifacts) > 0,
        "artifacts": list(ml_artifacts.keys()),
    }


@app.post("/predict/hotspot", response_model=HotspotResponse, tags=["Inference"])
def predict_hotspot(payload: ObservationPayload):
    """
    Evaluates current atmospheric, spatial, and fire indicators to classify hotspot emergence.
    """
    if "hotspot" not in ml_artifacts:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Hotspot classifier is not loaded.",
        )

    model_bundle = ml_artifacts["hotspot"]
    model = model_bundle.get("model") if isinstance(model_bundle, dict) else model_bundle

    input_dict = payload.model_dump()
    df_input = pd.DataFrame([input_dict])

    try:
        if hasattr(model, "predict_proba"):
            prob = float(model.predict_proba(df_input)[:, 1][0])
        elif hasattr(model, "predict"):
            prob = float(model.predict(df_input)[0])
        else:
            prob = 0.5
    except Exception:
        # Fallback if specific trained feature subset is expected
        if isinstance(model_bundle, dict) and "features" in model_bundle:
            feats = model_bundle["features"]
            sub_df = pd.DataFrame([{f: input_dict.get(f, 0.0) for f in feats}])
            prob = float(model.predict_proba(sub_df)[:, 1][0])
        else:
            prob = 0.5

    is_hotspot = prob >= 0.50
    confidence = "HIGH" if prob > 0.75 or prob < 0.25 else "MODERATE"

    return HotspotResponse(
        is_hotspot=is_hotspot,
        probability=round(prob, 4),
        confidence_level=confidence,
        epistemic_uncertainty_flag=1 if 0.40 <= prob <= 0.60 else 0,
        model_version="hotspot_classifier_v1",
    )


@app.get("/api/v1/ml/hotspot/model-info", tags=["Inference"])
def get_hotspot_model_info():
    if "hotspot" not in ml_artifacts:
        return {
            "status": "MODEL_UNAVAILABLE",
            "modelVersion": "hotspot_classifier_v1",
            "engineType": "ML",
            "loaded": False,
        }
    art = ml_artifacts["hotspot"]
    return {
        "status": "OPERATIONAL",
        "modelVersion": "hotspot_classifier_v1",
        "engineType": "ML",
        "loaded": True,
        "algorithm": str(art.get("algorithm", "CalibratedClassifierCV")),
        "featureCount": len(art.get("feature_cols", [])),
        "operationalThreshold": float(art.get("operational_threshold", 0.20)),
        "supportedDomain": "Pune Metropolitan Region",
    }


@app.post("/api/v1/ml/hotspot/predict", response_model=HotspotInferenceResponse, tags=["Inference"])
def predict_hotspot_v1(req: HotspotInferenceRequest):
    """
    Evaluates a 36-feature vector against the calibrated hotspot_classifier_v1 model.
    Enforces model domain (Pune only).
    """
    if "hotspot" not in ml_artifacts:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="MODEL_UNAVAILABLE: Hotspot classifier artifact is not loaded.",
        )

    art = ml_artifacts["hotspot"]
    model = art["model"]
    cols = art["feature_cols"]
    threshold = float(art.get("operational_threshold", 0.20))
    model_version = "hotspot_classifier_v1"
    engine_type = "ML"

    h3_index = req.h3Index or req.h3_index or "unknown"
    city_name = req.cityName or req.city_name or "Pune"

    # Enforce Model Domain: Pune Metropolitan Region ONLY
    if city_name.strip().lower() not in ["pune", "pmr", "pune metropolitan region"]:
        return HotspotInferenceResponse(
            h3Index=h3_index,
            riskScore=0.0,
            riskLevel="LOW",
            confidence=0.0,
            isHotspot=False,
            operationalThreshold=threshold,
            modelVersion=model_version,
            engineType=engine_type,
            status="MODEL_DOMAIN_UNSUPPORTED",
            message=f"Model domain unsupported: hotspot_classifier_v1 was trained exclusively on Pune Metropolitan Region telemetry and is not validated for {city_name}.",
            metadata={
                "h3Index": h3_index,
                "cityName": city_name,
                "domainSupported": False,
            },
        )

    # Extract 36 features
    ordered_values = req.features
    feature_map = req.featureMap or req.feature_map

    if ordered_values is not None:
        if len(ordered_values) != 36:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"INSUFFICIENT_DATA: Expected exactly 36 features, got {len(ordered_values)}",
            )
        row_dict = {cols[i]: float(ordered_values[i]) for i in range(36)}
    elif feature_map is not None:
        row_dict = {}
        missing = []
        for col in cols:
            if col not in feature_map or feature_map[col] is None:
                missing.append(col)
            else:
                row_dict[col] = float(feature_map[col])
        if missing:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"INSUFFICIENT_DATA: Missing required model features: {missing}",
            )
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="INSUFFICIENT_DATA: Neither 'features' array nor 'featureMap' provided.",
        )

    # Validate finite values
    for k, v in row_dict.items():
        if np.isnan(v) or np.isinf(v):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"INVALID_MODEL_OUTPUT: Non-finite value in feature '{k}': {v}",
            )

    # Build DataFrame in exact artifact order
    df = pd.DataFrame([row_dict])[cols]

    # Predict probability
    try:
        proba = model.predict_proba(df)[0]
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"MODEL_INFERENCE_FAILED: {str(e)}",
        )

    classes = getattr(model, "classes_", np.array([0, 1]))
    pos_idx_arr = np.where(classes == 1)[0]
    pos_idx = int(pos_idx_arr[0]) if len(pos_idx_arr) > 0 else 1

    calibrated_prob = float(proba[pos_idx])
    if calibrated_prob < 0.0 or calibrated_prob > 1.0 or np.isnan(calibrated_prob):
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"INVALID_MODEL_OUTPUT: Calibrated probability out of bounds: {calibrated_prob}",
        )

    is_hotspot = calibrated_prob >= threshold

    # Map risk level
    if calibrated_prob < 0.20:
        risk_level = "LOW"
    elif calibrated_prob < 0.40:
        risk_level = "MODERATE"
    elif calibrated_prob < 0.70:
        risk_level = "HIGH"
    else:
        risk_level = "CRITICAL"

    # Compute hyperlocal confidence
    conf_res = compute_prediction_confidence(row_dict, calibrated_prob)
    overall_conf = float(conf_res.get("overall_confidence", 0.85))

    return HotspotInferenceResponse(
        h3Index=h3_index,
        riskScore=round(calibrated_prob, 4),
        riskLevel=risk_level,
        confidence=round(overall_conf, 4),
        isHotspot=is_hotspot,
        operationalThreshold=threshold,
        modelVersion=model_version,
        engineType=engine_type,
        status="SUCCESS",
        metadata={
            "h3Index": h3_index,
            "cityName": city_name,
            "algorithm": str(art.get("algorithm", "CalibratedClassifierCV")),
            "confidenceFactors": conf_res,
            "calibratedProbability": round(calibrated_prob, 4),
            "positiveClassIndex": pos_idx,
            "domainSupported": True,
        },
    )


@app.post("/predict/forecast", response_model=ForecastResponse, tags=["Inference"])
def predict_forecast(payload: ObservationPayload):
    """
    Forecasts future PM2.5 levels at T+1, T+3, and T+6 hours ahead.
    """
    if "forecast" not in ml_artifacts:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Multi-horizon forecaster is not loaded.",
        )

    bundle = ml_artifacts["forecast"]
    base_pm25 = payload.pm25 or 45.0

    forecasts: Dict[str, float] = {}

    if isinstance(bundle, dict) and "models" in bundle:
        models = bundle["models"]
        input_data = pd.DataFrame([payload.model_dump()])
        for horizon in [1, 3, 6]:
            h_key = f"t_plus_{horizon}"
            if h_key in models:
                try:
                    forecasts[f"T+{horizon}h"] = round(float(models[h_key].predict(input_data)[0]), 2)
                except Exception:
                    forecasts[f"T+{horizon}h"] = round(base_pm25 * (1.0 + (horizon * 0.02)), 2)
            else:
                forecasts[f"T+{horizon}h"] = round(base_pm25 * (1.0 + (horizon * 0.02)), 2)
    else:
        # Heuristic fallback if bundle structure differs
        forecasts = {
            "T+1h": round(base_pm25 * 1.02, 2),
            "T+3h": round(base_pm25 * 1.05, 2),
            "T+6h": round(base_pm25 * 1.08, 2),
        }

    max_pred = max(forecasts.values())
    advisory = "Satisfactory" if max_pred < 60 else "Moderate" if max_pred < 120 else "Poor / Hazardous"

    return ForecastResponse(
        station_id=payload.station_id,
        forecast_pm25=forecasts,
        advisory=advisory,
    )