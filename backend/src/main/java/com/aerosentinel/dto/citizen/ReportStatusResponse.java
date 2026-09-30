package com.aerosentinel.dto.citizen;

public record ReportStatusResponse(
        String reportId,
        String h3CellId,
        String status
) {}