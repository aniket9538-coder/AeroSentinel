package com.aerosentinel.forecast.feature;

/**
 * Explicit semantics for physical feature source provenance.
 * Distinguishes genuine physical measurements, legitimate physical zeros,
 * missing telemetry, unavailable external sources, and model baseline imputations.
 */
public enum FeatureProvenance {
    /** Direct, valid physical sensor or numerical observation */
    VALID_OBSERVATION,

    /** Genuine physical zero confirmed by domain context (e.g. 0 active fires in 24h, 0 mm rain) */
    REAL_ZERO,

    /** Telemetry or sensor observation missing at time T0 */
    MISSING,

    /** Data source provider or station connection is offline / unavailable */
    SOURCE_UNAVAILABLE,

    /** Imputed baseline value substituted for model readiness (NOT an actual physical observation) */
    IMPUTED_BASELINE
}
