package com.aerosentinel.alert;

import com.aerosentinel.dto.alert.AuthorityQueueItemDto;
import com.aerosentinel.dto.inspection.AssignTeamRequest;
import com.aerosentinel.dto.inspection.FieldVerificationDto;
import com.aerosentinel.dto.inspection.InspectionResponseDto;
import com.aerosentinel.inspection.InspectionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Authoritative REST Controller for Feature 5 (F5-P5/P6) Authority Alerts & Queue.
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;
    private final InspectionService inspectionService;

    public AlertController(AlertService alertService) {
        this(alertService, null);
    }

    @Autowired
    public AlertController(AlertService alertService, @Autowired(required = false) InspectionService inspectionService) {
        this.alertService = alertService;
        this.inspectionService = inspectionService;
    }

    /**
     * Authoritative Authority Queue endpoint returning rich, evidence-backed alert candidates.
     */
    @GetMapping("/authority")
    public ResponseEntity<List<AuthorityQueueItemDto>> getAuthorityQueue(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(alertService.getAuthorityQueue(status, cityId));
    }

    /**
     * Retrieves a single authoritative Alert candidate by UUID.
     */
    @GetMapping("/{alertId}")
    public ResponseEntity<AuthorityQueueItemDto> getAlertById(@PathVariable UUID alertId) {
        return alertService.getAuthorityAlertById(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Acknowledges an alert candidate.
     */
    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<AuthorityQueueItemDto> acknowledgeAlert(
            @PathVariable UUID alertId,
            @RequestParam(required = false) UUID userId
    ) {
        enforceAuthorityAuthorization();
        Alert updated = alertService.acknowledgeAlert(alertId, userId);
        return ResponseEntity.ok(alertService.toAuthorityQueueItemDto(updated));
    }

    /**
     * Resolves an alert candidate.
     */
    @PatchMapping("/{alertId}/resolve")
    public ResponseEntity<AuthorityQueueItemDto> resolveAlert(
            @PathVariable UUID alertId,
            @RequestParam(required = false) UUID userId,
            @RequestBody(required = false) com.aerosentinel.dto.action.ResolveAlertRequest request
    ) {
        enforceAuthorityAuthorization();
        String notes = request != null ? request.resolutionNotes() : null;
        UUID uid = (request != null && request.resolvedBy() != null) ? request.resolvedBy() : userId;
        boolean strict = request != null;
        Alert updated = alertService.resolveAlert(alertId, uid, notes, strict);
        return ResponseEntity.ok(alertService.toAuthorityQueueItemDto(updated));
    }

    /**
     * Dismisses an alert candidate.
     */
    @PatchMapping("/{alertId}/dismiss")
    public ResponseEntity<AuthorityQueueItemDto> dismissAlert(
            @PathVariable UUID alertId,
            @RequestParam(required = false) UUID userId,
            @RequestBody(required = false) com.aerosentinel.dto.action.DismissAlertRequest request
    ) {
        enforceAuthorityAuthorization();
        String reason = (request != null && request.dismissalReason() != null)
                ? request.dismissalReason()
                : "Dismissed by municipal authority";
        UUID uid = (request != null && request.dismissedBy() != null) ? request.dismissedBy() : userId;
        Alert updated = alertService.dismissAlert(alertId, uid, reason);
        return ResponseEntity.ok(alertService.toAuthorityQueueItemDto(updated));
    }

    // =========================================================================
    // F5-P6 FIELD TEAM ASSIGNMENT & VERIFICATION INTEGRATION
    // =========================================================================

    /**
     * Assigns a field team to an acknowledged alert candidate (F5-P6).
     */
    @PostMapping("/{alertId}/assign")
    public ResponseEntity<InspectionResponseDto> assignFieldTeam(
            @PathVariable UUID alertId,
            @Valid @RequestBody AssignTeamRequest request
    ) {
        enforceAuthorityAuthorization();
        if (inspectionService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inspectionService.assignFieldTeam(alertId, request));
    }

    /**
     * Retrieves current active assignment for an alert candidate (F5-P6).
     */
    @GetMapping("/{alertId}/assignment")
    public ResponseEntity<InspectionResponseDto> getAlertAssignment(@PathVariable UUID alertId) {
        if (inspectionService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
        return inspectionService.getActiveAssignmentForAlert(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all field verification records for an alert (F5-P6).
     */
    @GetMapping("/{alertId}/verifications")
    public ResponseEntity<List<FieldVerificationDto>> getAlertVerifications(@PathVariable UUID alertId) {
        if (inspectionService == null) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
        return ResponseEntity.ok(inspectionService.getVerificationsForAlert(alertId));
    }

    // =========================================================================
    // Legacy compatibility endpoints
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<Alert>> getAlerts(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(defaultValue = "OPEN") String status
    ) {
        if (cityId != null) {
            return ResponseEntity.ok(alertService.getAlertsByCity(cityId, status));
        }
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<Alert> createAlert(@RequestBody Alert alert) {
        return ResponseEntity.ok(alertService.createAlert(alert));
    }

    private void enforceAuthorityAuthorization() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            boolean hasAuthorityRole = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_AUTHORITY") || a.getAuthority().equals("ROLE_ADMIN"));
            if (!hasAuthorityRole) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied: Authority operational actions require AUTHORITY or ADMIN role");
            }
        }
    }
}
