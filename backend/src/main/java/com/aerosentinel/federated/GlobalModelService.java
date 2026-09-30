package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.GlobalModelResponse;
import com.aerosentinel.dto.federated.ModelCatalogItemResponse;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GlobalModelService {

    private final FederatedGlobalModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    public GlobalModelService(FederatedGlobalModelRepository modelRepository, ObjectMapper objectMapper) {
        this.modelRepository = modelRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GlobalModelResponse getActiveModel() {
        FederatedGlobalModel model = modelRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active global model configured"));
        return toGlobalModelResponse(model);
    }

    @Transactional(readOnly = true)
    public List<ModelCatalogItemResponse> getCatalog() {
        return modelRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toCatalogItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GlobalModelResponse getModelByVersion(String modelVersion) {
        FederatedGlobalModel model = modelRepository.findByVersion(modelVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Global model not found: " + modelVersion));
        return toGlobalModelResponse(model);
    }

    private GlobalModelResponse toGlobalModelResponse(FederatedGlobalModel m) {
        Map<String, Double> metrics = new HashMap<>();
        if (m.getMae() != null) metrics.put("mae", m.getMae());
        if (m.getRmse() != null) metrics.put("rmse", m.getRmse());
        if (m.getRocAuc() != null) metrics.put("rocAuc", m.getRocAuc());
        if (m.getBrierScore() != null) metrics.put("brierScore", m.getBrierScore());

        List<String> nodes = parseNodes(m.getParticipatingNodes());

        return new GlobalModelResponse(
                m.getVersion(),
                m.getBaseModelVersion(),
                m.getRoundId(),
                m.isActive(),
                nodes,
                m.getTotalSamples() != null ? m.getTotalSamples() : 0,
                metrics,
                m.getArtifactPath(),
                m.getCreatedAt()
        );
    }

    private ModelCatalogItemResponse toCatalogItemResponse(FederatedGlobalModel m) {
        Map<String, Double> metrics = new HashMap<>();
        if (m.getMae() != null) metrics.put("mae", m.getMae());
        if (m.getRmse() != null) metrics.put("rmse", m.getRmse());
        if (m.getRocAuc() != null) metrics.put("rocAuc", m.getRocAuc());
        if (m.getBrierScore() != null) metrics.put("brierScore", m.getBrierScore());

        List<String> nodes = parseNodes(m.getParticipatingNodes());

        return new ModelCatalogItemResponse(
                m.getVersion(),
                m.getBaseModelVersion(),
                m.getRoundId(),
                m.isActive(),
                nodes,
                m.getTotalSamples() != null ? m.getTotalSamples() : 0,
                metrics,
                m.getArtifactPath(),
                m.getCreatedAt()
        );
    }

    private List<String> parseNodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
