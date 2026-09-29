package com.aerosentinel.evidence;

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
import java.util.*;
import java.util.concurrent.*;

/**
 * Robust Spring Boot Process Bridge for F5 Evidence Orchestration.
 *
 * Responsibilities:
 * 1. Executes Python F5 CLI subprocess safely with strict JSON piping over STDIN.
 * 2. Enforces bounded 15-second execution timeout with process cleanup.
 * 3. Captures and parses strict JSON STDOUT response into EvidenceAiOutputDto.
 * 4. Never fabricates values; provides graceful controlled fallback on failure.
 */
@Component
public class EvidenceAiClient {

    private static final Logger log = LoggerFactory.getLogger(EvidenceAiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final String pythonCommand;
    private final String cliPath;
    private final long timeoutMs;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvidenceAiOutputDto(
            String status,
            String h3Index,
            String cityId,
            String predictionId,
            String eventId,
            String canonicalEventId,
            List<String> clusterH3Cells,
            Double evidenceScore,
            Double evidenceCompleteness,
            String consistency,
            String triageState,
            ScoreBreakdownDto scoreBreakdown,
            Map<String, String> sourceMatrix,
            List<String> unavailableSources,
            List<String> conflictingNotes,
            List<String> supportingSignals,
            List<SignalItemDto> signals,
            AiInterpretationDto aiInterpretation,
            RecommendedVerificationDto recommendedVerification,
            ProvenanceDto provenance
    ) {
        public record ScoreBreakdownDto(
                Double observation_strength,
                Double ml_forecast_support,
                Double multi_source_agreement,
                Double spatial_consistency,
                Double temporal_persistence,
                Double recency_factor,
                Double conflict_penalty,
                Double evidence_completeness,
                Double final_evidence_score
        ) {}

        public record SignalItemDto(
                String signalId,
                String sourceType,
                String description,
                String timestamp,
                Double confidenceScore,
                String dataSource,
                String relevanceTier,
                String sourceRef
        ) {}

        public record AiInterpretationDto(
                String summaryPublic,
                String summaryAnalyst,
                String detectedCondition,
                List<String> supportingSignals,
                String forecastTrajectory,
                String uncertaintyStatement,
                List<String> unsupportedConclusions,
                Boolean causalClaimSupported,
                Boolean isGrounded,
                String modelVersion,
                String promptVersion
        ) {}

        public record RecommendedVerificationDto(
                String action,
                String priority,
                List<String> guidelines
        ) {}

        public record ProvenanceDto(
                String h3Index,
                String cityId,
                String f3PredictionId,
                String f3ModelVersion,
                String f4ModelVersion,
                String f5ScoringVersion,
                String geminiModelVersion,
                String geminiPromptVersion,
                String evaluatedAt
        ) {}
    }

    public EvidenceAiClient(
            @Value("${app.evidence.ai.python-command:python}") String pythonCommand,
            @Value("${app.evidence.ai.cli-path:ai-service/ml/inference/orchestrate_evidence_cli.py}") String cliPath,
            @Value("${app.evidence.ai.timeout-ms:15000}") long timeoutMs
    ) {
        this.pythonCommand = pythonCommand;
        this.cliPath = cliPath;
        this.timeoutMs = timeoutMs;
    }

    public EvidenceAiOutputDto evaluate(Map<String, Object> inputPayload) {
        Path scriptPath = resolveCliPath(cliPath);

        String inputJson;
        try {
            inputJson = MAPPER.writeValueAsString(inputPayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize evidence CLI input payload: " + e.getMessage(), e);
        }

        return executeProcess(scriptPath, inputJson);
    }

    private EvidenceAiOutputDto executeProcess(Path scriptPath, String inputJson) {
        ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptPath.toString());
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            log.error("Failed to start evidence inference process using '{}': {}", pythonCommand, e.getMessage());
            throw new RuntimeException("Failed to start Python process: " + e.getMessage(), e);
        }

        ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = ioExecutor.submit(() -> readStream(process.getInputStream()));
        Future<String> stderrFuture = ioExecutor.submit(() -> readStream(process.getErrorStream()));

        try (OutputStream os = process.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.write(inputJson);
            writer.flush();
        } catch (IOException e) {
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            throw new RuntimeException("Failed to pipe input to evidence inference process: " + e.getMessage(), e);
        }

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            throw new RuntimeException("Evidence inference thread interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            log.error("Evidence CLI timed out after {} ms", timeoutMs);
            throw new RuntimeException("Evidence inference timed out after " + timeoutMs + " ms");
        }

        String stdout;
        String stderr;
        try {
            stdout = stdoutFuture.get(2, TimeUnit.SECONDS);
            stderr = stderrFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to retrieve stdout/stderr after process exit: {}", e.getMessage());
            stdout = "";
            stderr = "";
        } finally {
            ioExecutor.shutdown();
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("Evidence CLI failed with exit code {}. Stderr: {}", exitCode, stderr);
            throw new RuntimeException("Evidence CLI failed with exit code " + exitCode + ": " + stderr);
        }

        try {
            return MAPPER.readValue(stdout, EvidenceAiOutputDto.class);
        } catch (Exception e) {
            log.error("Failed to parse evidence CLI stdout JSON. Stdout: '{}', Error: {}", stdout, e.getMessage());
            throw new RuntimeException("Failed to parse evidence CLI output: " + e.getMessage(), e);
        }
    }

    private Path resolveCliPath(String pathStr) {
        Path p = Paths.get(pathStr);
        if (Files.exists(p)) {
            return p.toAbsolutePath();
        }
        Path fromParent = Paths.get("..", pathStr);
        if (Files.exists(fromParent)) {
            return fromParent.toAbsolutePath();
        }
        Path fromAiService = Paths.get("ai-service", "ml", "inference", "orchestrate_evidence_cli.py");
        if (Files.exists(fromAiService)) {
            return fromAiService.toAbsolutePath();
        }
        return p.toAbsolutePath();
    }

    private String readStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (IOException e) {
            log.warn("Error reading stream from Python process: {}", e.getMessage());
            return "";
        }
    }
}
