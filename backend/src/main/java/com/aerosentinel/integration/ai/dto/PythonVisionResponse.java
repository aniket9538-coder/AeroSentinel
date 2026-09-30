package com.aerosentinel.integration.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PythonVisionResponse(
        @JsonProperty("modelName") String modelName,
        @JsonProperty("detectedCategory") String detectedCategory,
        @JsonProperty("visibleCondition") String visibleCondition,
        @JsonProperty("confidence") Double confidence,
        @JsonProperty("visualIndicators") List<String> visualIndicators,
        @JsonProperty("narrativeSummary") String narrativeSummary,
        @JsonProperty("verificationRequired") Boolean verificationRequired,
        @JsonProperty("privacyFlags") Map<String, Object> privacyFlags,
        @JsonProperty("limitations") String limitations,
        @JsonProperty("rawResponse") Map<String, Object> rawResponse
) {}