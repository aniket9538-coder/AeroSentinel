package com.aerosentinel.authority;

import com.aerosentinel.dto.authority.AuthorityQueueItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/authority/queue")
@PreAuthorize("hasAnyRole('AUTHORITY', 'ADMIN', 'OPERATOR')")
public class AuthorityQueueController {

    private final AuthorityQueueService authorityQueueService;

    public AuthorityQueueController(AuthorityQueueService authorityQueueService) {
        this.authorityQueueService = authorityQueueService;
    }

    /**
     * Get the active authority queue of alerts requiring field response.
     * GET /api/v1/authority/queue?cityId={cityId}&severity=HIGH&status=OPEN
     */
    @GetMapping
    public ResponseEntity<List<AuthorityQueueItemResponse>> getQueue(
            @RequestParam(value = "cityId", required = false) UUID cityId,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "status", required = false, defaultValue = "OPEN") String status
    ) {
        return ResponseEntity.ok(authorityQueueService.getAuthorityQueue(cityId, severity, status));
    }
}