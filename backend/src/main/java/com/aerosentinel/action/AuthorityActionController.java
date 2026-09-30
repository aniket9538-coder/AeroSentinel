package com.aerosentinel.action;

import com.aerosentinel.dto.authority.ActionResponse;
import com.aerosentinel.dto.authority.RecordActionRequest;
import com.aerosentinel.dto.authority.ResolveEventRequest;
import com.aerosentinel.dto.event.PollutionEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('AUTHORITY', 'ADMIN', 'OPERATOR')")
public class AuthorityActionController {

    private final AuthorityActionService authorityActionService;

    public AuthorityActionController(AuthorityActionService authorityActionService) {
        this.authorityActionService = authorityActionService;
    }

    /**
     * Record an operational action and transition event to ACTION_TAKEN.
     * POST /api/v1/events/{eventId}/actions
     */
    @PostMapping("/events/{eventId}/actions")
    public ResponseEntity<ActionResponse> recordAction(
            @PathVariable("eventId") String eventId,
            @Valid @RequestBody RecordActionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authorityActionService.recordAction(eventId, request));
    }

    /**
     * Get all recorded actions for an event.
     * GET /api/v1/events/{eventId}/actions
     */
    @GetMapping("/events/{eventId}/actions")
    public ResponseEntity<List<ActionResponse>> getActions(@PathVariable("eventId") String eventId) {
        return ResponseEntity.ok(authorityActionService.getActionsForEvent(eventId));
    }

    /**
     * Resolve an event and close open alerts.
     * POST /api/v1/events/{eventId}/resolve
     */
    @PostMapping("/events/{eventId}/resolve")
    public ResponseEntity<PollutionEventResponse> resolveEvent(
            @PathVariable("eventId") String eventId,
            @Valid @RequestBody ResolveEventRequest request
    ) {
        return ResponseEntity.ok(authorityActionService.resolveEvent(eventId, request));
    }
}