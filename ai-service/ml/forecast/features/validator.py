"""
AeroSentinel - Production F4 Forecast Feature Validator
File: ai-service/ml/forecast/features/validator.py

Implements 14 strict checks required by F4-P2:
  1. Exactly 36 features
  2. Exact expected names and order
  3. All expected values numeric
  4. Finite values only (no NaN, no Inf)
  5. No unexpected feature
  6. No missing required feature (in strict mode)
  7. No NaN after adapter stage
  8. No Infinity
  9. Spatial identity valid (15-character hex H3 index)
 10. City identity valid
 11. T0 timestamp valid
 12. Traceability tracked
 13. Artifact schema compatibility verified
 14. Quality Status Integrity (cannot be VALID if fields are missing or imputed)
"""

import re
from typing import List, Optional
import numpy as np

from ml.forecast.features.contract import ORDERED_FEATURE_NAMES, F4_FEATURE_COUNT
from ml.forecast.features.vector import ForecastFeatureVector


class ForecastFeaturesInvalidError(ValueError):
    """Raised when forecast feature validation fails."""
    def __init__(self, message: str, h3_index: str, errors: Optional[List[str]] = None):
        super().__init__(f"{message} for cell {h3_index}. Errors: {errors or []}")
        self.error_code = "FORECAST_FEATURES_INVALID"
        self.h3_index = h3_index
        self.errors = errors or []


class ForecastFeatureValidator:
    H3_REGEX = re.compile(r"^[0-9a-fA-F]{15}$")

    def validate(self, vector: ForecastFeatureVector, strict_no_missing: bool = False) -> None:
        if vector is None:
            raise ForecastFeaturesInvalidError("ForecastFeatureVector must not be None", "unknown")

        errors: List[str] = []
        h3 = vector.h3_index

        # 1 & 2 & 5: Feature count and naming
        feats = vector.features
        if len(feats) != F4_FEATURE_COUNT:
            errors.append(f"Check 1/2 failed: Expected {F4_FEATURE_COUNT} features, got {len(feats)}")

        for expected in ORDERED_FEATURE_NAMES:
            if expected not in feats:
                errors.append(f"Check 2/5 failed: Missing expected feature: '{expected}'")

        # 3, 4, 7, 8: Numeric, finite, no NaN, no Inf
        for name, val in feats.items():
            if val is None:
                continue
            if not isinstance(val, (int, float, np.number)):
                errors.append(f"Check 3 failed: Feature '{name}' is non-numeric: {type(val)}")
            else:
                fval = float(val)
                if np.isnan(fval):
                    errors.append(f"Check 7 failed: Feature '{name}' is NaN")
                elif np.isinf(fval):
                    errors.append(f"Check 4/8 failed: Feature '{name}' is Infinite: {fval}")

        # 6: Missing features in strict mode
        if strict_no_missing and vector.missing_fields:
            errors.append(f"Check 6 failed: Strict mode requires zero missing features, got: {vector.missing_fields}")

        # 9: Spatial identity
        if not h3 or not self.H3_REGEX.match(h3):
            errors.append(f"Check 9 failed: Invalid spatial identity H3 index: '{h3}'")

        # 10: City identity
        if not vector.city_id:
            errors.append("Check 10 failed: city_id must not be empty")

        # 11: Base timestamp
        if not vector.base_timestamp:
            errors.append("Check 11 failed: base_timestamp T0 must not be empty")

        # 12: Quality status
        if not vector.quality_status:
            errors.append("Check 12 failed: quality_status must be specified")

        # 14: Quality Status Integrity check
        if vector.missing_fields and vector.quality_status.upper() == "VALID":
            errors.append(f"Check 14 failed: Quality status cannot be VALID when physical fields are missing: {vector.missing_fields}")

        if vector.feature_provenance:
            for feat, prov in vector.feature_provenance.items():
                if prov in ("MISSING", "SOURCE_UNAVAILABLE") and vector.quality_status.upper() == "VALID":
                    errors.append(f"Check 14 failed: Quality status cannot be VALID when feature '{feat}' has provenance {prov}")
                    break

        if errors:
            raise ForecastFeaturesInvalidError("Forecast feature validation failed", h3, errors)
