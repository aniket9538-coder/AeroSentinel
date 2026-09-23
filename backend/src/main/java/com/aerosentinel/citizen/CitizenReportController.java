package com.aerosentinel.citizen;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/citizen")
public class CitizenReportController {

    private final CitizenReportService citizenReportService;

    public CitizenReportController(CitizenReportService citizenReportService) {
        this.citizenReportService = citizenReportService;
    }

    @GetMapping("/reports")
    public ResponseEntity<List<CitizenReport>> getReports(@RequestParam UUID cityId) {
        return ResponseEntity.ok(citizenReportService.getReportsByCity(cityId));
    }

    @PostMapping("/reports")
    public ResponseEntity<CitizenReport> submitReport(@RequestBody CitizenReport report) {
        return ResponseEntity.ok(citizenReportService.createReport(report));
    }
}
