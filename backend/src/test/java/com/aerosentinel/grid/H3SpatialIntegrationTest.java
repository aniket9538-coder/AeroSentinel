package com.aerosentinel.grid;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.integration.provider.IngestionService;
import com.aerosentinel.integration.provider.IngestionSummary;
import com.aerosentinel.integration.provider.ProviderObservation;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3BackfillService;
import com.aerosentinel.spatial.H3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class H3SpatialIntegrationTest {

    @Autowired
    private H3BackfillService backfillService;

    @Autowired
    private H3Service h3Service;

    @Autowired
    private GridService gridService;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private AirObservationRepository airObservationRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private IngestionService ingestionService;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID DELHI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");

    @Test
    @Order(1)
    @DisplayName("Integration 1: Initial startup or manual backfill populates all 166 air observations with non-null H3")
    void testBackfillAllAirObservations() {
        // Run backfill (or verify state after startup runner)
        H3BackfillService.BackfillSummary summary = backfillService.backfillAirObservationsAndGridCells();

        long totalObs = airObservationRepository.count();
        assertThat(totalObs).isEqualTo(166L);

        long nullH3Count = airObservationRepository.countByH3IndexIsNull();
        assertThat(nullH3Count).isEqualTo(0L);

        List<AirObservation> allObs = airObservationRepository.findAll();
        assertThat(allObs).hasSize(166);
        for (AirObservation obs : allObs) {
            assertThat(obs.getH3Index()).isNotNull();
            assertThat(obs.getH3Index()).isNotBlank();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Integration 2: All 166 H3 indexes are valid Uber H3 Resolution 8 indexes")
    void testAllH3IndexesValid() {
        List<AirObservation> allObs = airObservationRepository.findAll();
        assertThat(allObs).isNotEmpty();

        for (AirObservation obs : allObs) {
            assertThat(h3Service.validateH3Index(obs.getH3Index()))
                    .withFailMessage("Observation %s has invalid H3 index: %s", obs.getId(), obs.getH3Index())
                    .isTrue();
            assertThat(h3Service.getCellResolution(obs.getH3Index())).isEqualTo(8);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Integration 3: Unique H3 cells match the 8 monitoring stations exactly")
    void testUniqueH3CellsCreated() {
        List<AirObservation> allObs = airObservationRepository.findAll();
        Set<String> uniqueH3InObs = allObs.stream()
                .map(AirObservation::getH3Index)
                .collect(Collectors.toSet());

        // There are 8 monitoring stations across Pune, Mumbai, Delhi
        assertThat(uniqueH3InObs).hasSize(8);

        List<GridCell> allGridCells = gridRepository.findAll();
        assertThat(allGridCells).hasSize(8);

        Set<String> gridCellH3s = allGridCells.stream()
                .map(GridCell::getH3Index)
                .collect(Collectors.toSet());
        assertThat(gridCellH3s).isEqualTo(uniqueH3InObs);
    }

    @Test
    @Order(4)
    @DisplayName("Integration 4: City relationships preserved across Pune, Mumbai, and Delhi")
    void testCityRelationshipsPreserved() {
        List<GridCell> puneCells = gridRepository.findByCityId(PUNE_CITY_ID);
        List<GridCell> mumbaiCells = gridRepository.findByCityId(MUMBAI_CITY_ID);
        List<GridCell> delhiCells = gridRepository.findByCityId(DELHI_CITY_ID);

        assertThat(puneCells).hasSize(3);   // PUN-001, PUN-002, PUN-003
        assertThat(mumbaiCells).hasSize(2); // MUM-001, MUM-002
        assertThat(delhiCells).hasSize(3);  // DEL-001, DEL-002, DEL-003

        // Verify air observations match their stations' respective city H3 cells
        List<AirObservation> puneObs = airObservationRepository.findByCityIdOrderByObservedAtDesc(PUNE_CITY_ID);
        assertThat(puneObs).hasSize(36);
        Set<String> puneExpectedH3 = puneCells.stream().map(GridCell::getH3Index).collect(Collectors.toSet());
        for (AirObservation obs : puneObs) {
            assertThat(puneExpectedH3).contains(obs.getH3Index());
        }

        List<AirObservation> mumbaiObs = airObservationRepository.findByCityIdOrderByObservedAtDesc(MUMBAI_CITY_ID);
        assertThat(mumbaiObs).hasSize(52);
        Set<String> mumbaiExpectedH3 = mumbaiCells.stream().map(GridCell::getH3Index).collect(Collectors.toSet());
        for (AirObservation obs : mumbaiObs) {
            assertThat(mumbaiExpectedH3).contains(obs.getH3Index());
        }

        List<AirObservation> delhiObs = airObservationRepository.findByCityIdOrderByObservedAtDesc(DELHI_CITY_ID);
        assertThat(delhiObs).hasSize(78);
        Set<String> delhiExpectedH3 = delhiCells.stream().map(GridCell::getH3Index).collect(Collectors.toSet());
        for (AirObservation obs : delhiObs) {
            assertThat(delhiExpectedH3).contains(obs.getH3Index());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Integration 5: PostGIS geometry populated with valid Polygon boundaries for every grid cell")
    void testPostGisGeometryPopulated() {
        List<GridCell> allGridCells = gridRepository.findAll();
        assertThat(allGridCells).hasSize(8);

        for (GridCell cell : allGridCells) {
            Optional<String> wktOpt = gridRepository.findBoundaryWktByH3Index(cell.getH3Index());
            assertThat(wktOpt).isPresent();
            String wkt = wktOpt.get();
            assertThat(wkt).startsWith("POLYGON((").endsWith("))");

            Boolean valid = gridRepository.isBoundaryValid(cell.getH3Index());
            assertThat(valid).withFailMessage("Boundary for cell %s is not valid PostGIS geometry", cell.getH3Index())
                    .isTrue();

            assertThat(cell.getCenterLatitude()).isNotNull();
            assertThat(cell.getCenterLongitude()).isNotNull();
            assertThat(cell.getResolution()).isEqualTo(8);
        }

        long nonNullCount = gridRepository.countCellsWithNonNullBoundary();
        assertThat(nonNullCount).isEqualTo(8L);
    }

    @Test
    @Order(6)
    @DisplayName("Integration 6: Rerun of backfill is completely idempotent (0 additional updates, 0 duplicate cells)")
    void testRerunIsIdempotent() {
        long obsCountBefore = airObservationRepository.count();
        long gridCountBefore = gridRepository.count();

        H3BackfillService.BackfillSummary secondRun = backfillService.backfillAirObservationsAndGridCells();

        assertThat(secondRun.observationsUpdated()).isEqualTo(0);
        assertThat(secondRun.gridCellsCreated()).isEqualTo(0);

        long obsCountAfter = airObservationRepository.count();
        long gridCountAfter = gridRepository.count();

        assertThat(obsCountAfter).isEqualTo(obsCountBefore);
        assertThat(gridCountAfter).isEqualTo(gridCountBefore);
    }

    @Test
    @Order(7)
    @DisplayName("Integration 7: F1 observation fields (pm25, timestamps, source, quality) remain 100% untouched")
    void testF1DataIntegrityUntouched() {
        // Verify Pune latest observation values from V5 baseline
        Optional<AirObservation> pun1Latest = airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-001");
        assertThat(pun1Latest).isPresent();
        assertThat(pun1Latest.get().getPm25()).isEqualTo(78.0);
        assertThat(pun1Latest.get().getSource()).isEqualTo("CPCB");
        assertThat(pun1Latest.get().getDataQuality()).isEqualTo("VALID");
        assertThat(pun1Latest.get().getObservedAt()).isEqualTo(Instant.parse("2026-09-24T22:00:00Z"));

        Optional<AirObservation> pun2Latest = airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-002");
        assertThat(pun2Latest).isPresent();
        assertThat(pun2Latest.get().getPm25()).isEqualTo(62.0);

        Optional<AirObservation> pun3Latest = airObservationRepository.findFirstByStationIdOrderByObservedAtDesc("PUN-003");
        assertThat(pun3Latest).isPresent();
        assertThat(pun3Latest.get().getPm25()).isEqualTo(86.0);
    }

    @Test
    @Order(8)
    @DisplayName("Integration 8: Ingestion integration - new real air observation receives H3 index and creates/reuses grid cell")
    void testNewAirIngestionReceivesH3AndGridCell() {
        Instant testTime = Instant.parse("2026-09-25T11:00:00Z");
        ProviderObservation newObs = new ProviderObservation(
                "8118", // maps to PUN-001
                18.5314, 73.8446,
                testTime,
                45.5,
                "OPENAQ",
                "VALID"
        );

        try {
            IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(newObs));
            assertThat(summary.getInserted()).isEqualTo(1);

            List<AirObservation> insertedList = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-001");
            AirObservation inserted = insertedList.stream()
                    .filter(o -> testTime.equals(o.getObservedAt()))
                    .findFirst()
                    .orElseThrow();

            assertThat(inserted.getH3Index()).isNotNull();
            assertThat(h3Service.validateH3Index(inserted.getH3Index())).isTrue();
            assertThat(inserted.getPm25()).isEqualTo(45.5);

            // Verify grid cell exists and is reused
            Optional<GridCell> cellOpt = gridRepository.findByH3Index(inserted.getH3Index());
            assertThat(cellOpt).isPresent();
            assertThat(cellOpt.get().getCityId()).isEqualTo(PUNE_CITY_ID);

            // Clean up test observation
            airObservationRepository.delete(inserted);
        } finally {
            // Guarantee cleanup
            airObservationRepository.findAll().stream()
                    .filter(o -> testTime.equals(o.getObservedAt()) && "PUN-001".equals(o.getStationId()))
                    .forEach(airObservationRepository::delete);
        }
    }
}
