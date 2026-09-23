package com.aerosentinel.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class PollutionEventController {

    private final PollutionEventService eventService;

    public PollutionEventController(PollutionEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<PollutionEvent>> getEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PollutionEvent> getEvent(@PathVariable UUID id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
