package com.aerosentinel.spatial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Startup runner ensuring that all existing real F1 observations have authoritative
 * H3 indexes and real grid_cells records exist upon application launch.
 * Guaranteed to be idempotent.
 */
@Component
public class H3BackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(H3BackfillRunner.class);

    private final H3BackfillService backfillService;

    public H3BackfillRunner(H3BackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            H3BackfillService.BackfillSummary summary = backfillService.backfillAirObservationsAndGridCells();
            log.info("H3BackfillRunner executed successfully: updated {} observations, created {} grid cells",
                    summary.observationsUpdated(), summary.gridCellsCreated());
        } catch (Exception e) {
            log.error("H3BackfillRunner failed during startup spatial initialization", e);
            throw e;
        }
    }
}
