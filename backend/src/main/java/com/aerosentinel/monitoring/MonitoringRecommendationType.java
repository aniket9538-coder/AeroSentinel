package com.aerosentinel.monitoring;

/**
 * Machine-readable recommendation classifications for F8 monitoring decision support.
 *
 * NOTE: These categories are F8 MVP implementation choices for operational triage,
 * not externally mandated regulatory requirements.
 */
public enum MonitoringRecommendationType {
    /**
     * Low priority cells: continue standard routine observation without immediate intervention.
     */
    ROUTINE_MONITORING,

    /**
     * Medium priority cells: closer observation or targeted monitoring recommended.
     */
    TARGETED_MONITORING,

    /**
     * High priority cells with monitoring coverage gap: prioritize deploying additional mobile monitoring.
     */
    MOBILE_SENSOR_RECOMMENDED,

    /**
     * High priority cells within existing station observation range: field verification and observation recommended.
     */
    FIELD_VERIFICATION_RECOMMENDED
}
