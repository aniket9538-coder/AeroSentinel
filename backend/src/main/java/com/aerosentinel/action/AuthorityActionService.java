package com.aerosentinel.action;

import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.alert.AlertStatus;
import com.aerosentinel.dto.authority.ActionResponse;
import com.aerosentinel.dto.authority.RecordActionRequest;
import com.aerosentinel.dto.authority.ResolveEventRequest;
import com.aerosentinel.dto.event.PollutionEventResponse;
import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventMapper;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.event.PollutionEventStatus;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthorityActionService {

    private static final Logger log = LoggerFactory.getLogger(AuthorityActionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthorityActionRepository actionRepository;
    private final PollutionEventRepository pollutionEventRepository;
    private final AlertRepository alertRepository;
    private final PollutionEventMapper eventMapper;

    public AuthorityActionService(
            AuthorityActionRepository actionRepository,
            PollutionEventRepository pollutionEventRepository,
            AlertRepository alertRepository,
            PollutionEventMapper eventMapper
    ) {
        this.actionRepository = actionRepository;
        this.pollutionEventRepository = pollutionEventRepository;
        this.alertRepository = alertRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public ActionResponse recordAction(String eventId, RecordActionRequest request) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (!PollutionEventStatus.canTransition(event.getStatus(), PollutionEventStatus.ACTION_TAKEN.name())) {
            throw new ValidationException("INVALID_EVENT_STATE: Event " + eventId +
                    " in status " + event.getStatus() + " cannot transition to ACTION_TAKEN");
        }

        ActionType actionType;
        try {
            actionType = ActionType.valueOf(request.actionType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_ACTION_TYPE: Supported types are FIELD_VERIFICATION, MOBILE_SENSOR_DEPLOYED, SITE_CHECK, EMISSION_STOP_NOTICE, MONITORING_INTENSIFIED");
        }

        String performedBy = resolveCurrentUserId();

        AuthorityAction action = new AuthorityAction();
        action.setActionId(generateActionId());
        action.setEventId(event.getId() != null ? event.getId().toString() : (event.getEventId() != null ? event.getEventId() : ""));
        action.setActionType(actionType.name());
        action.setPerformedBy(performedBy != null ? performedBy : "AUTHORITY_OFFICER");
        action.setNotes(request.notes());
        action.setResult(request.result());
        action.setPerformedAt(Instant.now());

        AuthorityAction saved = actionRepository.save(action);

        // Transition event to ACTION_TAKEN
        event.setStatus(PollutionEventStatus.ACTION_TAKEN.name());
        event.setUpdatedAt(Instant.now());
        pollutionEventRepository.save(event);

        log.info("AUTHORITY_ACTION_RECORDED actionId={} eventId={} type={}",
                saved.getActionId(), eventId, actionType);

        return toActionResponse(saved);
    }

    @Transactional
    public PollutionEventResponse resolveEvent(String eventId, ResolveEventRequest request) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (!PollutionEventStatus.canTransition(event.getStatus(), PollutionEventStatus.RESOLVED.name())) {
            throw new ValidationException("INVALID_EVENT_STATE: Event " + eventId +
                    " in status " + event.getStatus() + " cannot transition to RESOLVED");
        }

        event.setStatus(PollutionEventStatus.RESOLVED.name());
        event.setEndedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        PollutionEvent resolved = pollutionEventRepository.save(event);

        // Close all open or acknowledged alerts for this event
        List<Alert> alerts = alertRepository.findByEventIdOrderByCreatedAtDesc(event.getId());
        for (Alert alert : alerts) {
            if (!AlertStatus.CLOSED.name().equalsIgnoreCase(alert.getStatus())) {
                alert.setStatus(AlertStatus.CLOSED.name());
                alert.setClosedAt(Instant.now());
                alertRepository.save(alert);
            }
        }

        log.info("EVENT_RESOLVED eventId={} notes={}", eventId, request.resolutionNotes());
        return eventMapper.toResponse(resolved);
    }

    @Transactional(readOnly = true)
    public List<ActionResponse> getActionsForEvent(String eventId) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        String targetEventId = event.getId() != null ? event.getId().toString() : (event.getEventId() != null ? event.getEventId() : "");
        return actionRepository.findByEventIdOrderByPerformedAtDesc(targetEventId)
                .stream().map(this::toActionResponse).toList();
    }

    private ActionResponse toActionResponse(AuthorityAction a) {
        UUID eventUuid = null;
        if (a.getEventId() != null) {
            try {
                eventUuid = UUID.fromString(a.getEventId());
            } catch (IllegalArgumentException ignored) {}
        }

        return new ActionResponse(
                a.getActionId(),
                eventUuid != null ? eventUuid : UUID.randomUUID(),
                a.getActionType() != null ? a.getActionType() : "FIELD_VERIFICATION",
                a.getPerformedBy(),
                a.getNotes(),
                a.getResult(),
                a.getPerformedAt()
        );
    }

    private String resolveCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String generateActionId() {
        int code = 1000 + RANDOM.nextInt(9000);
        String candidate = "ACT-" + code;
        int attempts = 0;
        while (actionRepository.findByActionId(candidate).isPresent() && attempts < 10) {
            code = 1000 + RANDOM.nextInt(9000);
            candidate = "ACT-" + code;
            attempts++;
        }
        if (attempts >= 10) {
            candidate = "ACT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return candidate;
    }
}