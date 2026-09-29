package com.aerosentinel.inspection;

import com.aerosentinel.dto.inspection.AssignTeamRequest;
import com.aerosentinel.dto.inspection.FieldVerificationDto;
import com.aerosentinel.dto.inspection.InspectionResponseDto;
import com.aerosentinel.dto.inspection.SubmitVerificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Field Inspections & Field Verifications (F5-P6).
 */
@RestController
@RequestMapping("/api/v1/inspections")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    /**
     * Retrieves an inspection by UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InspectionResponseDto> getInspectionById(@PathVariable UUID id) {
        return ResponseEntity.ok(inspectionService.getInspectionById(id));
    }

    /**
     * Starts an assigned inspection, transitioning it to IN_PROGRESS.
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<InspectionResponseDto> startInspection(@PathVariable UUID id) {
        return ResponseEntity.ok(inspectionService.startInspection(id));
    }

    /**
     * Submits an observational field verification report for an IN_PROGRESS inspection.
     * Transitions inspection to COMPLETED.
     */
    @PostMapping(value = {"/{id}/verification", "/{id}/verify"})
    public ResponseEntity<InspectionResponseDto> submitVerification(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitVerificationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.submitVerification(id, request));
    }

    /**
     * Retrieves all field verification records for an inspection.
     */
    @GetMapping("/{id}/verifications")
    public ResponseEntity<List<FieldVerificationDto>> getVerificationsForInspection(@PathVariable UUID id) {
        return ResponseEntity.ok(inspectionService.getVerificationsForInspection(id));
    }

    /**
     * Direct inspection assignment endpoint for an alert.
     */
    @PostMapping("/alert/{alertId}/assign")
    public ResponseEntity<InspectionResponseDto> assignInspection(
            @PathVariable UUID alertId,
            @Valid @RequestBody AssignTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.assignFieldTeam(alertId, request));
    }
}
