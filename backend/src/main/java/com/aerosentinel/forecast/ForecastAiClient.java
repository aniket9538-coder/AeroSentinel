package com.aerosentinel.forecast;

import com.aerosentinel.forecast.feature.ForecastFeatureAdapter.ModelReadyFeatureVector;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Robust Spring Boot Process Bridge for F4-P3 Forecast Inference.
 *
 * Responsibilities:
 * 1. Safe process execution using ProcessBuilder (never raw shell strings).
 * 2. Feeds validated 36-feature vector and lineage context to predict_forecast_cli.py via STDIN.
 * 3. Enforces bounded execution timeout with automatic cleanup.
 * 4. Captures and parses strict JSON STDOUT response.
 * 5. Performs rigorous Java-side validation of model output before returning to domain service.
 */
@Component
public class ForecastAiClient {

    private static final Logger log = LoggerFactory.getLogger(ForecastAiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final Set<Integer> REQUIRED_HORIZONS = Set.of(1, 3, 6);

    private final String pythonCommand;
    private final String cliPath;
    private final long timeoutMs;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastInferenceResultDto(
            String modelVersion,
            String generatedAt,
            String h3Index,
            String cityId,
            String parentPredictionId,
            String featureSnapshotId,
            String status,
            List<ForecastItemDto> forecasts,
            Double forecastConfidence
    ) {
        public record ForecastItemDto(
                Integer horizonHours,
                String targetTime,
                Double predictedPm25,
                Double lowerBound,
                Double upperBound,
                String unit
        ) {}
    }

    public ForecastAiClient(
            @Value("${app.forecast.ai.python-command:python}") String pythonCommand,
            @Value("${app.forecast.ai.cli-path:ai-service/ml/inference/predict_forecast_cli.py}") String cliPath,
            @Value("${app.forecast.ai.timeout-ms:15000}") long timeoutMs
    ) {
        this.pythonCommand = pythonCommand;
        this.cliPath = cliPath;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Executes standalone multi-horizon inference via the Python CLI process bridge.
     */
    public ForecastInferenceResultDto predict(UUID parentPredictionId, ModelReadyFeatureVector featureVector) {
        return predict(parentPredictionId, featureVector != null ? featureVector.baseTimestamp() : null, featureVector);
    }

    /**
     * Executes standalone multi-horizon inference with explicit authoritative baseTimestamp (T0).
     */
    public ForecastInferenceResultDto predict(UUID parentPredictionId, Instant baseTimestamp, ModelReadyFeatureVector featureVector) {
        if (featureVector == null) {
            throw new ForecastException.ValidationFailed("ModelReadyFeatureVector must not be null");
        }
        if (featureVector.orderedValues() == null || featureVector.orderedValues().length != 36) {
            throw new ForecastException.ValidationFailed("Feature vector must contain exactly 36 values");
        }

        Path scriptPath = resolveCliPath(cliPath);
        Instant effectiveBase = baseTimestamp != null ? baseTimestamp : (featureVector.baseTimestamp() != null ? featureVector.baseTimestamp() : Instant.now());
        Map<String, Object> payload = buildCliPayload(parentPredictionId, effectiveBase, featureVector);

        String inputJson;
        try {
            inputJson = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ForecastException.ValidationFailed("Failed to serialize forecast CLI payload: " + e.getMessage());
        }

        return executeProcess(scriptPath, inputJson, parentPredictionId, featureVector);
    }

    private Map<String, Object> buildCliPayload(UUID parentPredictionId, Instant baseTimestamp, ModelReadyFeatureVector vector) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("h3Index", vector.h3Index());
        payload.put("cityId", vector.cityId() != null ? vector.cityId().toString() : null);
        payload.put("parentPredictionId", parentPredictionId != null ? parentPredictionId.toString() : null);
        payload.put("featureSnapshotId", vector.featureSnapshotId() != null ? vector.featureSnapshotId().toString() : null);
        payload.put("predictedAt", baseTimestamp != null ? baseTimestamp.toString() : Instant.now().toString());
        payload.put("features", vector.featureMap());
        return payload;
    }

    private ForecastInferenceResultDto executeProcess(
            Path scriptPath,
            String inputJson,
            UUID expectedParentId,
            ModelReadyFeatureVector vector
    ) {
        ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptPath.toString());
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            log.error("Failed to start forecast inference process using command '{}': {}", pythonCommand, e.getMessage());
            throw new ForecastException.AiUnavailable("Failed to start Python process: " + e.getMessage(), e);
        }

        // Asynchronously read stdout and stderr to prevent pipe buffer deadlocks
        ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = ioExecutor.submit(() -> readStream(process.getInputStream()));
        Future<String> stderrFuture = ioExecutor.submit(() -> readStream(process.getErrorStream()));

        // Write input to STDIN
        try (OutputStream os = process.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write(inputJson);
            writer.flush();
        } catch (IOException e) {
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            throw new ForecastException.AiUnavailable("Failed to pipe input to forecast inference process: " + e.getMessage(), e);
        }

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            throw new ForecastException.AiTimeout("Forecast inference process interrupted");
        }

        if (!finished) {
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            log.error("Forecast inference timed out after {} ms for H3: {}", timeoutMs, vector.h3Index());
            throw new ForecastException.AiTimeout("Forecast inference timed out after " + timeoutMs + " ms");
        }

        String stdout;
        String stderr;
        try {
            stdout = stdoutFuture.get(2, TimeUnit.SECONDS).trim();
            stderr = stderrFuture.get(2, TimeUnit.SECONDS).trim();
        } catch (Exception e) {
            stdout = "";
            stderr = "";
        } finally {
            ioExecutor.shutdownNow();
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Forecast CLI exited with code {}. Stderr: '{}', Stdout: '{}'", exitCode, stderr, stdout);
            if (exitCode == 2 || stdout.contains("FORECAST_MODEL_UNAVAILABLE")) {
                throw new ForecastException.AiUnavailable("Forecast model unavailable: " + (stdout.isEmpty() ? stderr : stdout));
            }
            if (exitCode == 3 || stdout.contains("FORECAST_MODEL_CONTRACT_INVALID") || stdout.contains("INVALID_INPUT")) {
                throw new ForecastException.ValidationFailed("Forecast input contract invalid: " + (stdout.isEmpty() ? stderr : stdout));
            }
            throw new ForecastException.AiUnavailable("Forecast inference failed with exit code " + exitCode + ": " + stderr);
        }

        if (stdout.isEmpty()) {
            throw new ForecastException.AiUnavailable("Forecast CLI returned empty STDOUT");
        }

        ForecastInferenceResultDto result;
        try {
            result = MAPPER.readValue(stdout, ForecastInferenceResultDto.class);
        } catch (Exception e) {
            log.error("Failed to parse forecast CLI response JSON: '{}'. Error: {}", stdout, e.getMessage());
            throw new ForecastException.AiUnavailable("Invalid JSON from forecast CLI: " + e.getMessage(), e);
        }

        validateInferenceResult(result, expectedParentId, vector);
        return result;
    }

    /**
     * Validates the Python inference output contract against all P3/P4 constraints.
     */
    public void validateInferenceResult(
            ForecastInferenceResultDto result,
            UUID expectedParentId,
            ModelReadyFeatureVector vector
    ) {
        if (result == null) {
            throw new ForecastException.ValidationFailed("Inference result must not be null");
        }
        if (!"SUCCESS".equalsIgnoreCase(result.status())) {
            throw new ForecastException.ValidationFailed("Forecast inference status was not SUCCESS: " + result.status());
        }
        if (!"forecast_regressors_v1".equals(result.modelVersion())) {
            throw new ForecastException.ValidationFailed("Unexpected modelVersion: " + result.modelVersion());
        }
        if (result.forecastConfidence() != null) {
            throw new ForecastException.ValidationFailed("Forecast confidence must be strictly null");
        }
        if (result.forecasts() == null || result.forecasts().size() != 3) {
            throw new ForecastException.ValidationFailed("Forecast result must contain exactly 3 horizons, got: " +
                    (result.forecasts() == null ? 0 : result.forecasts().size()));
        }

        // Validate lineage
        if (expectedParentId != null && result.parentPredictionId() != null) {
            if (!expectedParentId.toString().equalsIgnoreCase(result.parentPredictionId())) {
                throw new ForecastException.ValidationFailed("Lineage mismatch: expected parentId " + expectedParentId +
                        ", got " + result.parentPredictionId());
            }
        }
        if (vector.h3Index() != null && !vector.h3Index().equalsIgnoreCase(result.h3Index())) {
            throw new ForecastException.ValidationFailed("Lineage mismatch: expected H3 " + vector.h3Index() +
                    ", got " + result.h3Index());
        }

        // Validate horizons and bounds
        Set<Integer> seenHorizons = new HashSet<>();
        Instant previousTarget = null;

        for (ForecastInferenceResultDto.ForecastItemDto item : result.forecasts()) {
            if (item.horizonHours() == null || !REQUIRED_HORIZONS.contains(item.horizonHours())) {
                throw new ForecastException.ValidationFailed("Unsupported horizon: " + item.horizonHours());
            }
            if (!seenHorizons.add(item.horizonHours())) {
                throw new ForecastException.ValidationFailed("Duplicate horizon in forecast output: " + item.horizonHours());
            }
            if (item.predictedPm25() == null || Double.isNaN(item.predictedPm25()) || Double.isInfinite(item.predictedPm25())) {
                throw new ForecastException.ValidationFailed("predictedPm25 must be finite numeric");
            }
            if (item.lowerBound() == null || Double.isNaN(item.lowerBound()) || Double.isInfinite(item.lowerBound())) {
                throw new ForecastException.ValidationFailed("lowerBound must be finite numeric");
            }
            if (item.upperBound() == null || Double.isNaN(item.upperBound()) || Double.isInfinite(item.upperBound())) {
                throw new ForecastException.ValidationFailed("upperBound must be finite numeric");
            }
            if (item.lowerBound() < 0.0) {
                throw new ForecastException.ValidationFailed("Physical lower bound violated: lowerBound < 0 (" + item.lowerBound() + ")");
            }
            if (item.lowerBound() > item.predictedPm25()) {
                throw new ForecastException.ValidationFailed("Interval violated: lowerBound > predictedPm25 (" +
                        item.lowerBound() + " > " + item.predictedPm25() + ")");
            }
            if (item.predictedPm25() > item.upperBound()) {
                throw new ForecastException.ValidationFailed("Interval violated: predictedPm25 > upperBound (" +
                        item.predictedPm25() + " > " + item.upperBound() + ")");
            }
            if (!"ug/m3".equalsIgnoreCase(item.unit())) {
                throw new ForecastException.ValidationFailed("Invalid unit: expected 'ug/m3', got " + item.unit());
            }

            // Target time validation
            Instant target;
            try {
                target = Instant.parse(item.targetTime());
            } catch (DateTimeParseException e) {
                throw new ForecastException.ValidationFailed("Invalid targetTime ISO format: " + item.targetTime());
            }

            if (previousTarget != null && !target.isAfter(previousTarget)) {
                throw new ForecastException.ValidationFailed("Target timestamps not in ascending order: " +
                        previousTarget + " then " + target);
            }
            previousTarget = target;
        }

        if (!seenHorizons.equals(REQUIRED_HORIZONS)) {
            throw new ForecastException.ValidationFailed("Forecasts must cover exact horizons [1, 3, 6], found: " + seenHorizons);
        }
    }

    private Path resolveCliPath(String configuredPath) {
        Path direct = Paths.get(configuredPath);
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().normalize();
        }

        Path fromParent = Paths.get("..").resolve(configuredPath);
        if (Files.exists(fromParent)) {
            return fromParent.toAbsolutePath().normalize();
        }

        Path userDir = Paths.get(System.getProperty("user.dir", "."));
        Path candidate = userDir.resolve(configuredPath);
        if (Files.exists(candidate)) {
            return candidate.toAbsolutePath().normalize();
        }

        Path candidateParent = userDir.resolve("..").resolve(configuredPath);
        if (Files.exists(candidateParent)) {
            return candidateParent.toAbsolutePath().normalize();
        }

        throw new ForecastException.AiUnavailable("Forecast CLI script not found at configured path: " + configuredPath);
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
