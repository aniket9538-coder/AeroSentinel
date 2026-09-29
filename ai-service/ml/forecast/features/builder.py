"""
AeroSentinel - Production F4 Forecast Feature Builder (Python)
File: ai-service/ml/forecast/features/builder.py

Builds ForecastFeatureVector instances from payload dictionaries, database snapshots,
or feature dictionaries. Enforces spatial, temporal, and quality tracking.
"""

from datetime import datetime
from typing import Dict, Any, List, Optional
import pandas as pd

from ml.forecast.features.contract import ORDERED_FEATURE_NAMES, F4_FEATURE_COUNT
from ml.forecast.features.vector import ForecastFeatureVector
from ml.forecast.features.validator import ForecastFeatureValidator, ForecastFeaturesInvalidError

PHYSICAL_ZERO_FEATURES = {
    "fire_count_24h_25km", "fire_frp_sum_24h_25km", "fire_frp_mean_24h_25km",
    "fire_frp_distance_decay", "fire_upwind_alignment_score", "rainfall",
    "monitoring_coverage_gap_flag", "industrial_zone_within_2km_flag"
}


class ForecastFeatureBuilder:
    def __init__(self, validator: Optional[ForecastFeatureValidator] = None):
        self.validator = validator or ForecastFeatureValidator()

    def build_from_payload(self, payload: Dict[str, Any]) -> ForecastFeatureVector:
        """
        Builds a ForecastFeatureVector from an incoming API/bridge payload.
        Accepts:
          - h3Index / h3_index
          - cityId / city_id
          - timestamp / base_timestamp / observedAt
          - features (list of 36 values) OR featureMap / feature_map (dict)
          - qualityStatus / quality_status
          - missingFeatures / missing_fields
        """
        h3_index = str(payload.get("h3Index") or payload.get("h3_index") or "").strip()
        city_id = str(payload.get("cityId") or payload.get("city_id") or "pune").strip()
        raw_ts = payload.get("baseTimestamp") or payload.get("base_timestamp") or payload.get("observedAt") or payload.get("timestamp") or datetime.now()

        if isinstance(raw_ts, str):
            ts = pd.to_datetime(raw_ts, utc=True).to_pydatetime()
        elif isinstance(raw_ts, datetime):
            ts = raw_ts
        else:
            ts = datetime.now()

        snapshot_id = payload.get("featureSnapshotId") or payload.get("feature_snapshot_id")
        quality_status = payload.get("qualityStatus") or payload.get("quality_status") or "VALID"
        missing_fields = list(payload.get("missingFeatures") or payload.get("missing_fields") or [])

        # Extract features
        ordered_list = payload.get("features")
        feature_map = payload.get("featureMap") or payload.get("feature_map")

        if feature_map is None and isinstance(ordered_list, dict):
            feature_map = ordered_list
            ordered_list = None

        final_features: Dict[str, float] = {}
        provenance: Dict[str, str] = {}

        if ordered_list is not None and isinstance(ordered_list, list):
            if len(ordered_list) != F4_FEATURE_COUNT:
                raise ForecastFeaturesInvalidError(
                    f"Features array length mismatch: expected {F4_FEATURE_COUNT}, got {len(ordered_list)}",
                    h3_index
                )
            for i, name in enumerate(ORDERED_FEATURE_NAMES):
                val = ordered_list[i]
                if val is None:
                    final_features[name] = 0.0
                    provenance[name] = "MISSING"
                    if name not in missing_fields:
                        missing_fields.append(name)
                else:
                    fval = float(val)
                    final_features[name] = fval
                    if name in missing_fields:
                        provenance[name] = "IMPUTED_BASELINE"
                    elif fval == 0.0 and name in PHYSICAL_ZERO_FEATURES:
                        provenance[name] = "REAL_ZERO"
                    else:
                        provenance[name] = "VALID_OBSERVATION"

        elif feature_map is not None and isinstance(feature_map, dict):
            for name in ORDERED_FEATURE_NAMES:
                val = feature_map.get(name)
                if val is None:
                    if name not in missing_fields:
                        missing_fields.append(name)
                    final_features[name] = 0.0
                    provenance[name] = "MISSING"
                else:
                    fval = float(val)
                    final_features[name] = fval
                    if name in missing_fields:
                        provenance[name] = "IMPUTED_BASELINE"
                    elif fval == 0.0 and name in PHYSICAL_ZERO_FEATURES:
                        provenance[name] = "REAL_ZERO"
                    else:
                        provenance[name] = "VALID_OBSERVATION"
        else:
            raise ForecastFeaturesInvalidError(
                "Neither 'features' array nor 'featureMap' dictionary provided in payload",
                h3_index
            )

        # Quality integrity: do not promote missing/imputed records to VALID
        if missing_fields and quality_status.upper() == "VALID":
            quality_status = "UNAVAILABLE" if len(missing_fields) > 3 else "MISSING"

        vector = ForecastFeatureVector(
            city_id=city_id,
            h3_index=h3_index,
            base_timestamp=ts,
            features=final_features,
            quality_status=quality_status,
            missing_fields=missing_fields,
            feature_snapshot_id=str(snapshot_id) if snapshot_id else None,
            feature_provenance=provenance
        )

        self.validator.validate(vector)
        return vector
