package com.aerosentinel.event;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PollutionEventService {

    private final PollutionEventRepository eventRepository;

    public PollutionEventService(PollutionEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<PollutionEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<PollutionEvent> getEventById(UUID id) {
        return eventRepository.findById(id);
    }

    public PollutionEvent saveEvent(PollutionEvent event) {
        return eventRepository.save(event);
    }
}
