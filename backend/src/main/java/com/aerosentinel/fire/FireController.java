package com.aerosentinel.fire;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fires")
public class FireController {

    private final FireService fireService;

    public FireController(FireService fireService) {
        this.fireService = fireService;
    }

    @GetMapping
    public ResponseEntity<List<FireEvent>> getFires(@RequestParam UUID cityId) {
        return ResponseEntity.ok(fireService.getFiresByCity(cityId));
    }
}
