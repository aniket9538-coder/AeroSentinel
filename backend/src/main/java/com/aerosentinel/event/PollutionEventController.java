package com.aerosentinel.event;

import com.aerosentinel.dto.event.PollutionEventContextDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing the Authoritative Pollution Event Context (Feature 7).
 *
 * Prediction != Event != Alert != Action
 */
@RestController
@RequestMapping("/api/v1/events")
public class PollutionEventController {

    private final PollutionEventService eventService;

    public PollutionEventController(PollutionEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<PollutionEventContextDto>> getEvents(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) String h3Index
    ) {
        if (h3Index != null && !h3Index.isBlank()) {
            return ResponseEntity.ok(eventService.getEventsByH3(h3Index.trim()));
        }
        if (cityId != null) {
            return ResponseEntity.ok(eventService.getEventsByCity(cityId));
        }
        return ResponseEntity.ok(eventService.getAllEventContexts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PollutionEventContextDto> getEvent(@PathVariable UUID id) {
        return eventService.getEventContextById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
