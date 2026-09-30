package com.aerosentinel.authority;

import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.alert.AlertStatus;
import com.aerosentinel.dto.authority.AuthorityQueueItemResponse;
import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthorityQueueService {

    private final AlertRepository alertRepository;
    private final PollutionEventRepository pollutionEventRepository;

    public AuthorityQueueService(
            AlertRepository alertRepository,
            PollutionEventRepository pollutionEventRepository
    ) {
        this.alertRepository = alertRepository;
        this.pollutionEventRepository = pollutionEventRepository;
    }

    @Transactional(readOnly = true)
    public List<AuthorityQueueItemResponse> getAuthorityQueue(UUID cityId, String severity, String status) {
        AlertStatus alertStatus = AlertStatus.OPEN;
        if (status != null && !status.isBlank()) {
            try {
                alertStatus = AlertStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = alertStatus != null ? alertStatus.name() : "OPEN";
        List<Alert> alerts = cityId != null
                ? alertRepository.findByCityIdAndStatusOrderByCreatedAtDesc(cityId, statusStr)
                : alertRepository.findByStatusOrderByCreatedAtDesc(statusStr);

        if (severity != null && !severity.isBlank()) {
            String targetSeverity = severity.trim().toUpperCase();
            alerts = alerts.stream()
                    .filter(a -> a.getSeverity() != null && a.getSeverity().equalsIgnoreCase(targetSeverity))
                    .toList();
        }

        List<UUID> eventUuids = alerts.stream().map(Alert::getEventId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<UUID, PollutionEvent> eventMap = pollutionEventRepository.findAllById(eventUuids)
                .stream()
                .collect(Collectors.toMap(PollutionEvent::getId, Function.identity()));

        return alerts.stream().map(alert -> {
            PollutionEvent event = alert.getEventId() != null ? eventMap.get(alert.getEventId()) : null;
            return new AuthorityQueueItemResponse(
                    alert.getAlertId(),
                    event != null ? event.getEventId() : null,
                    event != null ? event.getCityId() : null,
                    alert.getH3CellId(),
                    alert.getSeverity(),
                    alert.getTitle(),
                    alert.getRiskScore(),
                    alert.getConfidence(),
                    event != null ? event.getStatus() : null,
                    alert.getStatus(),
                    alert.getCreatedAt()
            );
        }).toList();
    }
}