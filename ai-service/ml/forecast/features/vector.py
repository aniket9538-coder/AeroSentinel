"""
AeroSentinel - Production F4 Forecast Feature Vector
File: ai-service/ml/forecast/features/vector.py

Encapsulates the exact 36 numeric features with spatial/temporal context and explicit provenance.
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional, Any
import numpy as np

from ml.forecast.features.contract import ORDERED_FEATURE_NAMES, F4_FEATURE_COUNT


@dataclass
class ForecastFeatureVector:
    city_id: str
    h3_index: str
    base_timestamp: datetime
    features: Dict[str, float]
    quality_status: str = "VALID"
    missing_fields: List[str] = field(default_factory=list)
    feature_snapshot_id: Optional[str] = None
    feature_provenance: Dict[str, str] = field(default_factory=dict)

    def __post_init__(self):
        if not self.h3_index:
            raise ValueError("h3_index must not be empty")
        if not self.base_timestamp:
            raise ValueError("base_timestamp must not be empty")

    @property
    def ordered_values(self) -> np.ndarray:
        """Returns the 36 feature values in authoritative artifact order."""
        vals = []
        for name in ORDERED_FEATURE_NAMES:
            val = self.features.get(name, 0.0)
            vals.append(float(val) if val is not None else 0.0)
        return np.array(vals, dtype=np.float64)

    def to_2d_array(self) -> np.ndarray:
        """Returns a 2D numpy array with shape (1, 36) matching model batch input."""
        return self.ordered_values.reshape(1, -1)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "city_id": self.city_id,
            "h3_index": self.h3_index,
            "base_timestamp": self.base_timestamp.isoformat() if hasattr(self.base_timestamp, "isoformat") else str(self.base_timestamp),
            "feature_snapshot_id": self.feature_snapshot_id,
            "quality_status": self.quality_status,
            "missing_fields": list(self.missing_fields),
            "features": dict(self.features),
            "ordered_values": self.ordered_values.tolist(),
            "feature_provenance": dict(self.feature_provenance)
        }
