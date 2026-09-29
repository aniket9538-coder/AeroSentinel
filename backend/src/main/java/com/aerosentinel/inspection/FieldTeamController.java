package com.aerosentinel.inspection;

import com.aerosentinel.dto.inspection.FieldTeamDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Municipal Field Teams directory (F5-P6).
 */
@RestController
@RequestMapping("/api/v1/field-teams")
public class FieldTeamController {

    private final InspectionService inspectionService;

    public FieldTeamController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    /**
     * Lists available field teams, optionally filtered by cityId and/or status.
     */
    @GetMapping
    public ResponseEntity<List<FieldTeamDto>> getFieldTeams(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(inspectionService.listFieldTeams(cityId, status));
    }

    /**
     * Retrieves a single field team by UUID.
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<FieldTeamDto> getFieldTeamById(@PathVariable UUID teamId) {
        return ResponseEntity.ok(inspectionService.getFieldTeamById(teamId));
    }
}
