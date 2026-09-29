package com.aerosentinel.inspection;

import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.dto.inspection.AssignTeamRequest;
import com.aerosentinel.dto.inspection.FieldVerificationDto;
import com.aerosentinel.dto.inspection.InspectionResponseDto;
import com.aerosentinel.dto.inspection.SubmitVerificationRequest;
import com.aerosentinel.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("F5-P6: Inspection & Verification Service Unit Tests")
class InspectionUnitTest {

    private InspectionRepository inspectionRepository;
    private FieldTeamRepository fieldTeamRepository;
    private FieldVerificationRepository fieldVerificationRepository;
    private AlertRepository alertRepository;
    private InspectionService inspectionService;

    private static final UUID ALERT_ID = UUID.fromString("e12da246-dbea-4028-aa51-bb667fcca393");
    private static final UUID EVENT_ID = UUID.fromString("446928c2-6090-437d-a7c7-a9909e648403");
    private static final UUID PREDICTION_ID = UUID.fromString("1e91f203-1b9a-4886-a355-bd326c903625");
    private static final String H3_INDEX = "886196944dfffff";
    private static final UUID TEAM_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440001");
    private static final UUID INSPECTION_ID = UUID.fromString("880e8400-e29b-41d4-a716-446655440001");

    private Alert mockAlert;
    private FieldTeam mockTeam;

    @BeforeEach
    void setUp() {
        inspectionRepository = mock(InspectionRepository.class);
        fieldTeamRepository = mock(FieldTeamRepository.class);
        fieldVerificationRepository = mock(FieldVerificationRepository.class);
        alertRepository = mock(AlertRepository.class);

        inspectionService = new InspectionService(
                inspectionRepository,
                fieldTeamRepository,
                fieldVerificationRepository,
                alertRepository
        );

        mockAlert = new Alert();
        mockAlert.setId(ALERT_ID);
        mockAlert.setEventId(EVENT_ID);
        mockAlert.setPredictionId(PREDICTION_ID);
        mockAlert.setH3Index(H3_INDEX);
        mockAlert.setStatus("ACKNOWLEDGED");

        mockTeam = new FieldTeam();
        mockTeam.setId(TEAM_ID);
        mockTeam.setTeamCode("TEAM-PUN-01");
        mockTeam.setTeamName("Pune Municipal Rapid Response Team A");
        mockTeam.setStatus("AVAILABLE");

        when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(mockAlert));
        when(fieldTeamRepository.findById(TEAM_ID)).thenReturn(Optional.of(mockTeam));
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(i -> i.getArgument(0));
        when(fieldVerificationRepository.save(any(FieldVerification.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("1. Assignment creation succeeds for ACKNOWLEDGED alert")
    void test1_assignmentCreation_success() {
        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, Instant.now(), "Investigate industrial corridor");

        when(inspectionRepository.findActiveByAlertId(ALERT_ID)).thenReturn(Optional.empty());
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(invocation -> {
            Inspection ins = invocation.getArgument(0);
            ins.setId(INSPECTION_ID);
            return ins;
        });

        InspectionResponseDto result = inspectionService.assignFieldTeam(ALERT_ID, req);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(INSPECTION_ID);
        assertThat(result.alertId()).isEqualTo(ALERT_ID);
        assertThat(result.teamId()).isEqualTo(TEAM_ID);
        assertThat(result.status()).isEqualTo("ASSIGNED");
        verify(inspectionRepository, times(1)).save(any(Inspection.class));
    }

    @Test
    @DisplayName("2. Assignment rejected when alert is in OPEN status")
    void test2_assignmentRejection_whenAlertOpen() {
        mockAlert.setStatus("OPEN");
        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, null, null);

        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Alert must be ACKNOWLEDGED before assigning a field team");

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Duplicate assignment rejected if active assignment exists")
    void test3_duplicateAssignmentPrevention() {
        Inspection existingActive = new Inspection();
        existingActive.setId(UUID.randomUUID());
        existingActive.setStatus("ASSIGNED");

        when(inspectionRepository.findActiveByAlertId(ALERT_ID)).thenReturn(Optional.of(existingActive));
        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, null, null);

        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Alert already has an active field assignment");

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Assignment preserves complete lineage (Alert -> Event -> H3 -> Prediction)")
    void test4_assignmentLineage_preservesEventH3Prediction() {
        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, null, "Lineage test");
        when(inspectionRepository.findActiveByAlertId(ALERT_ID)).thenReturn(Optional.empty());

        ArgumentCaptor<Inspection> captor = ArgumentCaptor.forClass(Inspection.class);
        when(inspectionRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        inspectionService.assignFieldTeam(ALERT_ID, req);

        Inspection saved = captor.getValue();
        assertThat(saved.getAlertId()).isEqualTo(ALERT_ID);
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getH3Index()).isEqualTo(H3_INDEX);
        assertThat(saved.getPredictionId()).isEqualTo(PREDICTION_ID);
        assertThat(saved.getTeamId()).isEqualTo(TEAM_ID);
    }

    @Test
    @DisplayName("5. Inspection start transition from ASSIGNED to IN_PROGRESS")
    void test5_inspectionStartTransition_fromAssignedToInProgress() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setAlertId(ALERT_ID);
        inspection.setTeamId(TEAM_ID);
        inspection.setStatus("ASSIGNED");

        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(i -> i.getArgument(0));

        InspectionResponseDto response = inspectionService.startInspection(INSPECTION_ID);

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.startedAt()).isNotNull();
        verify(inspectionRepository).save(inspection);
    }

    @Test
    @DisplayName("6. Verification submission succeeds with OBSERVED conditions and completes inspection")
    void test6_verificationSubmission_success() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setAlertId(ALERT_ID);
        inspection.setEventId(EVENT_ID);
        inspection.setH3Index(H3_INDEX);
        inspection.setPredictionId(PREDICTION_ID);
        inspection.setStatus("IN_PROGRESS");

        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(i -> i.getArgument(0));
        when(fieldVerificationRepository.save(any(FieldVerification.class))).thenAnswer(i -> {
            FieldVerification fv = i.getArgument(0);
            fv.setId(UUID.randomUUID());
            return fv;
        });

        SubmitVerificationRequest req = new SubmitVerificationRequest(
                "CONFIRMED",
                "Heavy black smoke emission from brick kiln boiler stack",
                "Operating without scrubber system",
                "PHOTO-001, PHOTO-002",
                "Inspector A. Deshmukh",
                Instant.now()
        );

        InspectionResponseDto response = inspectionService.submitVerification(INSPECTION_ID, req);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedAt()).isNotNull();
        assertThat(response.latestVerification()).isNotNull();
        assertThat(response.latestVerification().verificationResult()).isEqualTo("CONFIRMED");
        assertThat(response.latestVerification().observedConditions()).contains("brick kiln boiler stack");
    }

    @Test
    @DisplayName("7. Invalid verification transition rejected when inspection is not IN_PROGRESS")
    void test7_invalidVerificationTransition_whenNotStarted() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setStatus("ASSIGNED"); // Not started yet!

        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
        SubmitVerificationRequest req = new SubmitVerificationRequest(
                "CONFIRMED",
                "Smoke plume observed",
                null, null, null, null
        );

        assertThatThrownBy(() -> inspectionService.submitVerification(INSPECTION_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inspection must be IN_PROGRESS to submit verification");

        verify(fieldVerificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Verification result supports CONFIRMED, REJECTED, and NEEDS_FOLLOW_UP")
    void test8_verificationResultPersistence_confirmedRejectedFollowUp() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setStatus("IN_PROGRESS");
        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(i -> i.getArgument(0));
        when(fieldVerificationRepository.save(any(FieldVerification.class))).thenAnswer(i -> i.getArgument(0));

        // Test CONFIRMED
        SubmitVerificationRequest req1 = new SubmitVerificationRequest("CONFIRMED", "Active fire", null, null, null, null);
        assertThat(inspectionService.submitVerification(INSPECTION_ID, req1).latestVerification().verificationResult())
                .isEqualTo("CONFIRMED");

        // Test REJECTED
        inspection.setStatus("IN_PROGRESS");
        SubmitVerificationRequest req2 = new SubmitVerificationRequest("REJECTED", "False alarm - clean air", null, null, null, null);
        assertThat(inspectionService.submitVerification(INSPECTION_ID, req2).latestVerification().verificationResult())
                .isEqualTo("REJECTED");

        // Test NEEDS_FOLLOW_UP
        inspection.setStatus("IN_PROGRESS");
        SubmitVerificationRequest req3 = new SubmitVerificationRequest("NEEDS_FOLLOW_UP", "Source inaccessible", null, null, null, null);
        assertThat(inspectionService.submitVerification(INSPECTION_ID, req3).latestVerification().verificationResult())
                .isEqualTo("NEEDS_FOLLOW_UP");

        // Test INVALID
        inspection.setStatus("IN_PROGRESS");
        SubmitVerificationRequest reqInvalid = new SubmitVerificationRequest("INVALID_STATUS", "Conditions", null, null, null, null);
        assertThatThrownBy(() -> inspectionService.submitVerification(INSPECTION_ID, reqInvalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid verification result");
    }

    @Test
    @DisplayName("9. Alert linkage is preserved on verification record")
    void test9_alertLinkage_correctAlertId() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setAlertId(ALERT_ID);
        inspection.setStatus("IN_PROGRESS");

        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
        ArgumentCaptor<FieldVerification> captor = ArgumentCaptor.forClass(FieldVerification.class);
        when(fieldVerificationRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        SubmitVerificationRequest req = new SubmitVerificationRequest("CONFIRMED", "Visible soot", null, null, null, null);
        inspectionService.submitVerification(INSPECTION_ID, req);

        assertThat(captor.getValue().getAlertId()).isEqualTo(ALERT_ID);
        assertThat(captor.getValue().getInspectionId()).isEqualTo(INSPECTION_ID);
    }

    @Test
    @DisplayName("10. Event and H3 lineage preserved in verification")
    void test10_eventH3LineagePreservation_inVerification() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setAlertId(ALERT_ID);
        inspection.setEventId(EVENT_ID);
        inspection.setH3Index(H3_INDEX);
        inspection.setPredictionId(PREDICTION_ID);
        inspection.setStatus("IN_PROGRESS");

        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));
        ArgumentCaptor<FieldVerification> captor = ArgumentCaptor.forClass(FieldVerification.class);
        when(fieldVerificationRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        SubmitVerificationRequest req = new SubmitVerificationRequest("CONFIRMED", "Observed smoke", null, null, null, null);
        inspectionService.submitVerification(INSPECTION_ID, req);

        FieldVerification saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getH3Index()).isEqualTo(H3_INDEX);
        assertThat(saved.getPredictionId()).isEqualTo(PREDICTION_ID);
    }

    @Test
    @DisplayName("11. Assignment to RESOLVED alert is rejected")
    void test11_resolvedAlertAssignmentRejection() {
        mockAlert.setStatus("RESOLVED");
        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, null, null);

        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign field team to a RESOLVED alert");

        verify(inspectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("12. Concurrent assignment protection handles DataIntegrityViolationException")
    void test12_concurrentAssignmentProtection_dataIntegrityViolation() {
        when(inspectionRepository.findActiveByAlertId(ALERT_ID)).thenReturn(Optional.empty());
        when(inspectionRepository.save(any(Inspection.class)))
                .thenThrow(new DataIntegrityViolationException("Unique active alert index violation"));

        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, null, null);

        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Concurrent assignment conflict");
    }

    @Test
    @DisplayName("13. Missing alert throws ResourceNotFoundException")
    void test13_missingAlertHandling_resourceNotFound() {
        UUID unknownAlertId = UUID.randomUUID();
        when(alertRepository.findById(unknownAlertId)).thenReturn(Optional.empty());
        AssignTeamRequest req = new AssignTeamRequest(TEAM_ID, null, null, null);

        assertThatThrownBy(() -> inspectionService.assignFieldTeam(unknownAlertId, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found with id");
    }

    @Test
    @DisplayName("14. Missing team throws ResourceNotFoundException")
    void test14_missingTeamHandling_resourceNotFound() {
        UUID unknownTeamId = UUID.randomUUID();
        when(fieldTeamRepository.findById(unknownTeamId)).thenReturn(Optional.empty());
        AssignTeamRequest req = new AssignTeamRequest(unknownTeamId, null, null, null);

        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Field team not found with id");
    }
}
