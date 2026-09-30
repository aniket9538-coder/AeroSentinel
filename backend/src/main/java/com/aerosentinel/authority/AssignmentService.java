package com.aerosentinel.authority;

import com.aerosentinel.dto.authority.AssignTeamRequest;
import com.aerosentinel.dto.authority.AssignmentResponse;
import com.aerosentinel.event.PollutionEvent;
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

import java.time.Instant;
import java.util.UUID;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final PollutionEventRepository pollutionEventRepository;
    private final AuthorityTeamRepository authorityTeamRepository;

    public AssignmentService(
            PollutionEventRepository pollutionEventRepository,
            AuthorityTeamRepository authorityTeamRepository
    ) {
        this.pollutionEventRepository = pollutionEventRepository;
        this.authorityTeamRepository = authorityTeamRepository;
    }

    @Transactional
    public AssignmentResponse assignTeamToEvent(String eventId, AssignTeamRequest request) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Pollution event not found: " + eventId));

        if (!PollutionEventStatus.canTransition(event.getStatus(), PollutionEventStatus.ASSIGNED.name())) {
            throw new ValidationException("INVALID_EVENT_STATE: Event " + eventId +
                    " in status " + event.getStatus() + " cannot be transitioned to ASSIGNED");
        }

        AuthorityTeam team = authorityTeamRepository.findByTeamId(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Authority team not found: " + request.teamId()));

        if (Boolean.FALSE.equals(team.getActive())) {
            throw new ValidationException("TEAM_INACTIVE: Authority team " + request.teamId() + " is currently inactive");
        }

        event.setStatus(PollutionEventStatus.ASSIGNED.name());
        event.setUpdatedAt(Instant.now());
        pollutionEventRepository.save(event);

        UUID assignedBy = resolveCurrentUserId();
        log.info("EVENT_ASSIGNED eventId={} teamId={} assignedBy={}", eventId, team.getTeamId(), assignedBy);

        return new AssignmentResponse(
                event.getEventId(),
                team.getTeamId(),
                team.getName(),
                event.getStatus(),
                assignedBy,
                Instant.now()
        );
    }

    private UUID resolveCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return UUID.fromString(auth.getName());
            }
        } catch (Exception ignored) {}
        return null;
    }
}