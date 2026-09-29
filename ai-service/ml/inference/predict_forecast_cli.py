"""
AeroSentinel - Standalone Forecast Inference CLI / Process Bridge (F4-P3)
File: ai-service/ml/inference/predict_forecast_cli.py

Provides a standalone, deterministic CLI for evaluating the trained multi-horizon
forecast regressors (forecast_regressors_v1.joblib) on a model-ready 36-feature input.

Input: JSON string via STDIN or first argument.
Output: Strict JSON string on STDOUT.
"""

import sys
import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from ml.forecast.engine import ForecastInferenceEngine, load_forecast_artifact


def main():
    try:
        if len(sys.argv) > 1 and sys.argv[1].strip():
            raw_input = sys.argv[1]
        else:
            raw_input = sys.stdin.read()

        if not raw_input or not raw_input.strip():
            err = {"status": "ERROR", "message": "INVALID_INPUT: Empty input payload"}
            print(json.dumps(err), file=sys.stderr)
            sys.exit(1)

        try:
            payload = json.loads(raw_input)
        except json.JSONDecodeError as e:
            err = {"status": "ERROR", "message": f"INVALID_INPUT: Malformed JSON: {e}"}
            print(json.dumps(err), file=sys.stderr)
            sys.exit(1)

        engine = ForecastInferenceEngine()
        result = engine.predict(payload)
        print(json.dumps(result, indent=2))
        sys.exit(0)

    except FileNotFoundError as e:
        err = {"status": "FORECAST_MODEL_UNAVAILABLE", "message": str(e)}
        print(json.dumps(err))
        sys.exit(2)
    except ValueError as e:
        err = {"status": "FORECAST_MODEL_CONTRACT_INVALID" if "CONTRACT" in str(e) else "INVALID_INPUT", "message": str(e)}
        print(json.dumps(err))
        sys.exit(3)
    except Exception as e:
        err = {"status": "MODEL_INFERENCE_FAILED", "message": str(e)}
        print(json.dumps(err))
        sys.exit(4)


if __name__ == "__main__":
    main()
