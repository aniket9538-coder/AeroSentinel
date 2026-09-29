package com.aerosentinel.event;

import java.util.Set;

/**
 * Authoritative Canonical Lifecycle State Machine for PollutionEvent (Feature 7 Phase 6).
 *
 * Target lifecycle:
 *   OPEN -> ASSIGNED -> IN_INSPECTION -> ACTION_TAKEN -> RESOLVED
 *
 * Alternative authorized path:
 *   OPEN -> DISMISSED
 */
public enum PollutionEventStatus {
    OPEN,
    ASSIGNED,
    IN_INSPECTION,
    ACTION_TAKEN,
    RESOLVED,
    DISMISSED;

    private static final Set<String> VALID_NAMES = Set.of(
            "OPEN", "ASSIGNED", "IN_INSPECTION", "ACTION_TAKEN", "RESOLVED", "DISMISSED"
    );

    public static boolean isValid(String status) {
        if (status == null) return false;
        return VALID_NAMES.contains(status.trim().toUpperCase());
    }

    /**
     * Validates if transitioning from currentStatus to targetStatus is permitted.
     * Throws IllegalStateException if the transition violates the domain state machine.
     */
    public static void validateTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("Current status and target status must not be null");
        }

        String cur = currentStatus.trim().toUpperCase();
        String tgt = targetStatus.trim().toUpperCase();

        if (cur.equals(tgt)) {
            return; // Idempotent
        }

        // Terminal states cannot transition to anything
        if ("RESOLVED".equals(cur)) {
            throw new IllegalStateException("Cannot transition from terminal state RESOLVED to " + tgt);
        }
        if ("DISMISSED".equals(cur)) {
            throw new IllegalStateException("Cannot transition from terminal state DISMISSED to " + tgt);
        }

        switch (cur) {
            case "OPEN":
                if (!"ASSIGNED".equals(tgt) && !"DISMISSED".equals(tgt) && !"RESOLVED".equals(tgt)) {
                    throw new IllegalStateException("Invalid transition from OPEN to " + tgt +
                            ". Valid next states are: ASSIGNED, DISMISSED");
                }
                break;

            case "ASSIGNED":
                if (!"IN_INSPECTION".equals(tgt) && !"DISMISSED".equals(tgt)) {
                    throw new IllegalStateException("Invalid transition from ASSIGNED to " + tgt +
                            ". Valid next states are: IN_INSPECTION, DISMISSED");
                }
                break;

            case "IN_INSPECTION":
                if (!"ACTION_TAKEN".equals(tgt) && !"RESOLVED".equals(tgt)) {
                    throw new IllegalStateException("Invalid transition from IN_INSPECTION to " + tgt +
                            ". Valid next states are: ACTION_TAKEN");
                }
                break;

            case "ACTION_TAKEN":
                if (!"RESOLVED".equals(tgt) && !"ACTION_TAKEN".equals(tgt)) {
                    throw new IllegalStateException("Invalid transition from ACTION_TAKEN to " + tgt +
                            ". Valid next state is: RESOLVED");
                }
                break;

            default:
                throw new IllegalStateException("Unknown lifecycle state: " + cur);
        }
    }
}
