package com.aerosentinel.monitoring;

/**
 * Authoritative classification levels for F8 monitoring priority.
 * Used to triage sensor deployment urgency based on combined risk,
 * prediction uncertainty, and distance from observation coverage.
 */
public enum MonitoringPriority {
    LOW,
    MEDIUM,
    HIGH
}
