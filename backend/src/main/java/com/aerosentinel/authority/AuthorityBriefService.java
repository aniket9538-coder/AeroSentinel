package com.aerosentinel.authority;

import com.aerosentinel.dto.authority.AuthorityBriefResponse;
import com.aerosentinel.event.EventEvidence;
import com.aerosentinel.event.EventEvidenceRepository;
import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class AuthorityBriefService {

    private static final Logger log = LoggerFactory.getLogger(AuthorityBriefService.class);

    private final PollutionEventRepository pollutionEventRepository;
    private final EventEvidenceRepository eventEvidenceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;

    public AuthorityBriefService(
            PollutionEventRepository pollutionEventRepository,
            EventEvidenceRepository eventEvidenceRepository,
            ObjectMapper objectMapper,
            @Value("${app.ai-service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${app.ai-service.timeout-ms:10000}") long timeoutMs
    ) {
        this.pollutionEventRepository = pollutionEventRepository;
        this.eventEvidenceRepository = eventEvidenceRepository;
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl;

        org.springframework.boot.web.client.RestTemplateBuilder builder = new org.springframework.boot.web.client.RestTemplateBuilder();
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public AuthorityBriefResponse generateAuthorityBrief(String eventId) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        List<EventEvidence> evidences = eventEvidenceRepository.findByPollutionEventIdOrderByObservedAtAsc(event.getId());

        List<Map<String, Object>> evidencePayload = evidences.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("evidenceType", e.getEvidenceType().name());
            map.put("summary", e.getValueSummary());
            map.put("strength", e.getStrength());
            map.put("observedAt", e.getObservedAt().toString());
            return map;
        }).toList();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("eventId", event.getEventId());
        requestBody.put("h3CellId", event.getH3Index());
        requestBody.put("riskScore", event.getRiskScore());
        requestBody.put("confidence", event.getConfidence());
        requestBody.put("evidences", evidencePayload);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String targetUrl = aiServiceUrl + "/api/v1/evidence/brief";
            ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                List<String> keyEvidences = new ArrayList<>();
                if (root.has("keyEvidence") && root.get("keyEvidence").isArray()) {
                    for (JsonNode node : root.get("keyEvidence")) {
                        keyEvidences.add(node.asText());
                    }
                }

                return new AuthorityBriefResponse(
                        eventId,
                        root.path("summary").asText("Potential particulate spike detected across multimodal inputs."),
                        keyEvidences,
                        root.path("recommendedVerification").asText("Deploy field team to inspect designated H3 perimeter."),
                        root.path("uncertainty").asText("Evidence context reflects observational indicators, not proof of source causality."),
                        "GENERATED",
                        Instant.now()
                );
            }
        } catch (Exception e) {
            log.warn("AI service unavailable for authority brief eventId={}: {}. Returning fallback brief.", eventId, e.getMessage());
        }

        // Resilient Fallback: Never fail event/alert operations if AI service is offline
        List<String> fallbackEvidence = evidences.stream()
                .map(e -> e.getEvidenceType() + ": " + e.getValueSummary())
                .toList();

        return new AuthorityBriefResponse(
                eventId,
                "Potential atmospheric pollution event detected in H3 cell " + event.getH3Index() + " (Risk: " + event.getRiskScore() + ").",
                fallbackEvidence.isEmpty() ? List.of("Automated threshold breach indicators registered.") : fallbackEvidence,
                "Field verification recommended. Visual or physical check required prior to regulatory action.",
                "AI reasoning engine unavailable. Brief generated from backend heuristic evidence context.",
                "FALLBACK",
                Instant.now()
        );
    }
}