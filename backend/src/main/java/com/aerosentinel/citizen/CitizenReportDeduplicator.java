package com.aerosentinel.citizen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Authoritative Citizen Report Deduplicator for Feature 6.
 *
 * Implements the locked spatio-temporal deduplication contract:
 * Same Uber H3 Resolution 8 Cell + within 60 minutes = duplicate / coalesced citizen evidence.
 *
 * Mirrors the Python implementation in ml/alert_support/citizen_dedup.py.
 */
@Component
public class CitizenReportDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(CitizenReportDeduplicator.class);

    public static final double MAX_TIME_DELTA_MINUTES = 60.0;
    public static final long MAX_TIME_DELTA_SECONDS = (long) (MAX_TIME_DELTA_MINUTES * 60);

    /**
     * Deduplicates and coalesces a list of citizen reports in the same spatio-temporal cluster.
     *
     * @param reports list of raw citizen reports (may contain multiple reports)
     * @return deduplicated list preserving the primary report per cluster
     */
    public List<CitizenReport> deduplicateReports(List<CitizenReport> reports) {
        if (reports == null || reports.size() <= 1) {
            return reports != null ? new ArrayList<>(reports) : Collections.emptyList();
        }

        List<CitizenReport> uniqueReports = new ArrayList<>();

        for (CitizenReport candidate : reports) {
            String candidateH3 = candidate.getH3Index();
            Instant candidateTime = candidate.getSubmittedAt() != null
                    ? candidate.getSubmittedAt()
                    : candidate.getCreatedAt();

            boolean isDuplicate = false;

            for (CitizenReport accepted : uniqueReports) {
                String acceptedH3 = accepted.getH3Index();
                Instant acceptedTime = accepted.getSubmittedAt() != null
                        ? accepted.getSubmittedAt()
                        : accepted.getCreatedAt();

                if (candidateH3 != null && acceptedH3 != null && candidateH3.equalsIgnoreCase(acceptedH3)) {
                    if (candidateTime != null && acceptedTime != null) {
                        long diffSeconds = Math.abs(Duration.between(candidateTime, acceptedTime).getSeconds());
                        if (diffSeconds <= MAX_TIME_DELTA_SECONDS) {
                            isDuplicate = true;
                            log.debug("Coalescing duplicate citizen report id={} with accepted report id={} in H3 {} (diff: {}s)",
                                    candidate.getId(), accepted.getId(), candidateH3, diffSeconds);
                            break;
                        }
                    }
                }
            }

            if (!isDuplicate) {
                uniqueReports.add(candidate);
            }
        }

        return uniqueReports;
    }
}
