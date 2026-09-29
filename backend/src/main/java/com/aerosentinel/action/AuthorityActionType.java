package com.aerosentinel.action;

import java.util.Set;

/**
 * Standard Authority Action Types (Feature 7 Phase 6).
 */
public enum AuthorityActionType {
    FIELD_VERIFICATION,
    MOBILE_SENSOR_DEPLOYED,
    SITE_CHECK,
    ADVISORY_ISSUED,
    RESOLUTION,
    DISMISSAL,
    OTHER;

    private static final Set<String> VALID_TYPES = Set.of(
            "FIELD_VERIFICATION",
            "MOBILE_SENSOR_DEPLOYED",
            "SITE_CHECK",
            "ADVISORY_ISSUED",
            "RESOLUTION",
            "DISMISSAL",
            "OTHER"
    );

    public static boolean isValid(String type) {
        if (type == null) return false;
        return VALID_TYPES.contains(type.trim().toUpperCase());
    }
}
