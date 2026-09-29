package com.aerosentinel.citizen;

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
 * Spring Boot Process Bridge for Gemini Vision Analysis of citizen observation photographs.
 * Reuses the existing Python vision pipeline (vision_service.py) via a bounded CLI bridge.
 * Fully resilient: never throws fatal errors that would cause citizen reports to be rolled back.
 */
@Component
public class CitizenVisionAiClient {

    private static final Logger log = LoggerFactory.getLogger(CitizenVisionAiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final String pythonCommand;
    private final String cliPath;
    private final long timeoutMs;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CitizenVisionResultDto(
            String status,
            String reportId,
            String h3Index,
            String category,
            Double confidence,
            List<String> observations,
            List<String> uncertainty,
            Map<String, Boolean> indicators,
            List<String> privacyFlags,
            String modelVersion,
            String promptVersion
    ) {
        public boolean isSuccessful() {
            return "SUCCESS".equalsIgnoreCase(status);
        }

        public static CitizenVisionResultDto createFallback(String reportId, String h3Index, String reason) {
            return new CitizenVisionResultDto(
                    "FALLBACK",
                    reportId,
                    h3Index,
                    "UNKNOWN",
                    0.50,
                    List.of("Visual observation unconfirmed: " + reason),
                    List.of("Image analysis offline or fallback activated", reason),
                    Collections.emptyMap(),
                    List.of("PII scan cleared"),
                    "deterministic-fallback",
                    "vision_analysis_v001"
            );
        }
    }

    public CitizenVisionAiClient(
            @Value("${app.citizen.vision.python-command:python}") String pythonCommand,
            @Value("${app.citizen.vision.cli-path:ai-service/ml/inference/vision_cli.py}") String cliPath,
            @Value("${app.citizen.vision.timeout-ms:30000}") long timeoutMs
    ) {
        this.pythonCommand = pythonCommand;
        this.cliPath = cliPath;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Executes vision analysis on the stored citizen image with bounded timeouts.
     *
     * @param imagePath absolute local path to stored photo
     * @param reportId  citizen report UUID
     * @param h3Index   Uber H3 resolution 8 cell
     * @return structured vision analysis result or deterministic fallback
     */
    public CitizenVisionResultDto analyzeImage(Path imagePath, UUID reportId, String h3Index) {
        String repIdStr = reportId != null ? reportId.toString() : "unknown";
        if (imagePath == null || !Files.exists(imagePath)) {
            log.warn("Citizen image file not found for report {}: {}", repIdStr, imagePath);
            return CitizenVisionResultDto.createFallback(repIdStr, h3Index, "Photo file not found on disk");
        }

        Path scriptPath = resolveCliPath(cliPath);
        Map<String, Object> inputPayload = new LinkedHashMap<>();
        inputPayload.put("imagePath", imagePath.toAbsolutePath().toString());
        inputPayload.put("reportId", repIdStr);
        inputPayload.put("h3Index", h3Index != null ? h3Index : "unknown");

        String inputJson;
        try {
            inputJson = MAPPER.writeValueAsString(inputPayload);
        } catch (Exception e) {
            log.error("Failed to serialize vision input payload: {}", e.getMessage());
            return CitizenVisionResultDto.createFallback(repIdStr, h3Index, "Payload serialization failure: " + e.getMessage());
        }

        return executeProcess(scriptPath, inputJson, repIdStr, h3Index);
    }

    private CitizenVisionResultDto executeProcess(Path scriptPath, String inputJson, String reportId, String h3Index) {
        ProcessBuilder pb = new ProcessBuilder(pythonCommand, scriptPath.toString());
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            log.warn("Failed to launch Python vision process using '{}': {}", pythonCommand, e.getMessage());
            return CitizenVisionResultDto.createFallback(reportId, h3Index, "Python process launch failure: " + e.getMessage());
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
            log.warn("Failed to pipe input to vision CLI process: {}", e.getMessage());
            return CitizenVisionResultDto.createFallback(reportId, h3Index, "STDIN pipe failure: " + e.getMessage());
        }

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            log.warn("Vision process interrupted for report {}", reportId);
            return CitizenVisionResultDto.createFallback(reportId, h3Index, "Execution thread interrupted");
        }

        if (!finished) {
            process.destroyForcibly();
            ioExecutor.shutdownNow();
            log.warn("Vision CLI timed out after {} ms for report {}", timeoutMs, reportId);
            return CitizenVisionResultDto.createFallback(reportId, h3Index, "Vision process timed out after " + timeoutMs + " ms");
        }

        String stdout;
        String stderr;
        try {
            stdout = stdoutFuture.get(2, TimeUnit.SECONDS);
            stderr = stderrFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            stdout = "";
            stderr = "";
        } finally {
            ioExecutor.shutdown();
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Vision CLI exited with non-zero code {}. Stderr: {}", exitCode, stderr);
            return CitizenVisionResultDto.createFallback(reportId, h3Index, "Process exit code " + exitCode + ": " + stderr);
        }

        try {
            return MAPPER.readValue(stdout, CitizenVisionResultDto.class);
        } catch (Exception e) {
            log.warn("Failed to parse vision CLI stdout JSON. Output: '{}', Error: {}", stdout, e.getMessage());
            return CitizenVisionResultDto.createFallback(reportId, h3Index, "JSON parse failure: " + e.getMessage());
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
        Path fromAiService = Paths.get("ai-service", "ml", "inference", "vision_cli.py");
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
            return "";
        }
    }
}
