package com.aerosentinel.citizen;

import com.aerosentinel.event.PollutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Spatio-temporal matcher linking Citizen Reports to canonical Pollution Events.
 *
 * Implements F6-P3 Matching Contracts:
 * 1. Spatial Matching: Strict Resolution 8 Uber H3 equality (report.h3Index == event.h3Index).
 * 2. Temporal Matching: Time delta <= TEMPORAL_MATCH_WINDOW_MINUTES (default 120 minutes / 2 hours).
 * 3. Event Preservation: Never fabricates an event if no spatial/temporal match is found.
 */
@Component
public class CitizenEventMatcher {

    private static final Logger log = LoggerFactory.getLogger(CitizenEventMatcher.class);

    /**
     * Authoritative temporal matching window (in minutes).
     * Corresponds to CAAQMS observation freshness and visual evidence validity window.
     */
    public static final long TEMPORAL_MATCH_WINDOW_MINUTES = 120L;

    /**
     * Tests whether a citizen report is both spatially and temporally matched to a pollution event.
     *
     * @param report citizen report with H3 index and submission timestamp
     * @param event  pollution event with H3 index, startedAt, and status
     * @return true if spatially and temporally compatible
     */
    public boolean isMatch(CitizenReport report, PollutionEvent event) {
        if (report == null || event == null) {
            return false;
        }

        // 1. Spatial Matching (Strict H3 Resolution 8 match)
        if (!isSpatialMatch(report.getH3Index(), event.getH3Index())) {
            return false;
        }

        // 2. Temporal Matching
        return isTemporalMatch(report, event);
    }

    /**
     * Checks strict H3 index equality.
     */
    public boolean isSpatialMatch(String reportH3, String eventH3) {
        if (reportH3 == null || eventH3 == null) {
            return false;
        }
        return reportH3.trim().equalsIgnoreCase(eventH3.trim());
    }

    /**
     * Checks if report timestamp falls within event temporal window.
     */
    public boolean isTemporalMatch(CitizenReport report, PollutionEvent event) {
        Instant reportTime = report.getSubmittedAt() != null
                ? report.getSubmittedAt()
                : report.getCreatedAt();

        Instant eventStarted = event.getStartedAt() != null
                ? event.getStartedAt()
                : event.getCreatedAt();

        if (reportTime == null || eventStarted == null) {
            return false;
        }

        // For OPEN events: report must be within TEMPORAL_MATCH_WINDOW_MINUTES of event start
        if ("OPEN".equalsIgnoreCase(event.getStatus()) || event.getResolvedAt() == null) {
            long minutesDelta = Math.abs(Duration.between(eventStarted, reportTime).toMinutes());
            boolean match = minutesDelta <= TEMPORAL_MATCH_WINDOW_MINUTES;
            if (!match) {
                log.debug("Temporal mismatch for open event {}: delta={}m > limit={}m",
                        event.getEventCode(), minutesDelta, TEMPORAL_MATCH_WINDOW_MINUTES);
            }
            return match;
        }

        // For RESOLVED events: report must fall between startedAt - 30m and resolvedAt + 30m
        Instant windowStart = eventStarted.minus(Duration.ofMinutes(30));
        Instant windowEnd = event.getResolvedAt().plus(Duration.ofMinutes(30));
        return !reportTime.isBefore(windowStart) && !reportTime.isAfter(windowEnd);
    }
}
