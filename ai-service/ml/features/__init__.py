"""
AeroSentinel - ML Feature Extraction & Assembly Package
"""
from .feature_service import (
    FEATURE_SCHEMA_VERSION,
    FEATURE_COUNT,
    ORDERED_FEATURE_NAMES,
    derive_wind_vectors,
    compute_leave_one_out_spatial_lag,
    compute_temporal_features,
    compute_monitoring_coverage,
    compute_fire_features,
    assemble_feature_vector,
)
