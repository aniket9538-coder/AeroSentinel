package com.aerosentinel.evidence;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping
    public ResponseEntity<List<EventEvidence>> getEvidence(@RequestParam UUID eventId) {
        return ResponseEntity.ok(evidenceService.getEvidenceByEvent(eventId));
    }
}
