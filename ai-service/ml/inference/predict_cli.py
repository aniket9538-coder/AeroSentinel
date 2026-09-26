"""
AeroSentinel - Standalone ML Hotspot Predictor CLI / Process Bridge
File: ai-service/ml/inference/predict_cli.py

Provides a robust, deterministic interface for evaluating the trained
hotspot_classifier_v1.joblib artifact on a 36-feature snapshot.

Input: JSON string via STDIN or first argument.
Output: JSON string on STDOUT.
"""

import sys
import json
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parent.parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

ARTIFACT_PATH = BASE_DIR / "models" / "artifacts" / "hotspot_classifier_v1.joblib"
_CACHED_ARTIFACT = None

import numpy as np
import pandas as pd
import joblib

from ml.inference.confidence import compute_prediction_confidence


def load_model():
    global _CACHED_ARTIFACT
    if _CACHED_ARTIFACT is not None:
        return _CACHED_ARTIFACT

    if not ARTIFACT_PATH.exists():
        raise FileNotFoundError(f"MODEL_UNAVAILABLE: Artifact not found at {ARTIFACT_PATH}")

    artifact = joblib.load(ARTIFACT_PATH)
    if "model" not in artifact or "feature_cols" not in artifact:
        raise ValueError("MODEL_LOAD_FAILED: Corrupted artifact structure")

    _CACHED_ARTIFACT = artifact
    return artifact


def run_inference(payload: dict) -> dict:
    artifact = load_model()
    model = artifact["model"]
    cols = artifact["feature_cols"]
    threshold = float(artifact.get("operational_threshold", 0.20))
    model_version = "hotspot_classifier_v1"
    engine_type = "ML"

    h3_index = payload.get("h3Index") or payload.get("h3_index") or "unknown"
    city_name = payload.get("cityName") or payload.get("city_name") or "Pune"

    # Enforce Model Domain: Pune Metropolitan Region ONLY
    if city_name.strip().lower() not in ["pune", "pmr", "pune metropolitan region"]:
        return {
            "h3Index": h3_index,
            "riskScore": 0.0,
            "riskLevel": "LOW",
            "confidence": 0.0,
            "isHotspot": False,
            "operationalThreshold": threshold,
            "modelVersion": model_version,
            "engineType": engine_type,
            "status": "MODEL_DOMAIN_UNSUPPORTED",
            "message": f"Model is trained exclusively for Pune Metropolitan Region. Unsupported for {city_name}.",
            "metadata": {
                "h3Index": h3_index,
                "cityName": city_name,
                "domainSupported": False
            }
        }

    # Extract 36 feature values
    ordered_values = payload.get("features")
    feature_map = payload.get("featureMap") or payload.get("feature_map")

    if ordered_values is not None:
        if len(ordered_values) != 36:
            raise ValueError(f"INSUFFICIENT_DATA: Expected exactly 36 features, got {len(ordered_values)}")
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
            raise ValueError(f"INSUFFICIENT_DATA: Missing required model features: {missing}")
    else:
        raise ValueError("INSUFFICIENT_DATA: Neither 'features' nor 'featureMap' provided")

    # Validate finite values
    for k, v in row_dict.items():
        if np.isnan(v) or np.isinf(v):
            raise ValueError(f"INVALID_MODEL_OUTPUT: Non-finite value in feature '{k}': {v}")

    # Build single-row DataFrame in exact artifact order
    df = pd.DataFrame([row_dict])[cols]

    # Predict probability
    proba = model.predict_proba(df)[0]
    classes = getattr(model, "classes_", np.array([0, 1]))
    pos_idx_arr = np.where(classes == 1)[0]
    pos_idx = int(pos_idx_arr[0]) if len(pos_idx_arr) > 0 else 1

    calibrated_prob = float(proba[pos_idx])
    if calibrated_prob < 0.0 or calibrated_prob > 1.0 or np.isnan(calibrated_prob):
        raise ValueError(f"INVALID_MODEL_OUTPUT: Calibrated probability out of bounds: {calibrated_prob}")

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

    return {
        "h3Index": h3_index,
        "riskScore": round(calibrated_prob, 4),
        "riskLevel": risk_level,
        "confidence": round(overall_conf, 4),
        "isHotspot": is_hotspot,
        "operationalThreshold": threshold,
        "modelVersion": model_version,
        "engineType": engine_type,
        "status": "SUCCESS",
        "metadata": {
            "h3Index": h3_index,
            "cityName": city_name,
            "algorithm": str(artifact.get("algorithm", "CalibratedClassifierCV")),
            "confidenceFactors": conf_res,
            "calibratedProbability": round(calibrated_prob, 4),
            "positiveClassIndex": pos_idx,
            "domainSupported": True
        }
    }


def main():
    try:
        if len(sys.argv) > 1 and sys.argv[1].strip():
            raw_input = sys.argv[1]
        else:
            raw_input = sys.stdin.read()

        if not raw_input.strip():
            print(json.dumps({"status": "ERROR", "message": "Empty input"}), file=sys.stderr)
            sys.exit(1)

        payload = json.loads(raw_input)
        result = run_inference(payload)
        print(json.dumps(result))
        sys.exit(0)

    except FileNotFoundError as e:
        err = {"status": "MODEL_UNAVAILABLE", "message": str(e)}
        print(json.dumps(err))
        sys.exit(2)
    except ValueError as e:
        err = {"status": "INSUFFICIENT_DATA" if "INSUFFICIENT" in str(e) else "INVALID_MODEL_OUTPUT", "message": str(e)}
        print(json.dumps(err))
        sys.exit(3)
    except Exception as e:
        err = {"status": "MODEL_INFERENCE_FAILED", "message": str(e)}
        print(json.dumps(err))
        sys.exit(4)


if __name__ == "__main__":
    main()
