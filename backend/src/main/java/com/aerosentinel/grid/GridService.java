package com.aerosentinel.grid;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GridService {

    private final GridRepository gridRepository;

    public GridService(GridRepository gridRepository) {
        this.gridRepository = gridRepository;
    }

    public List<GridCell> getCellsByCity(UUID cityId) {
        return gridRepository.findByCityId(cityId);
    }

    public Optional<GridCell> getCellByH3(String h3Index) {
        return gridRepository.findByH3Index(h3Index);
    }

    public GridCell saveCell(GridCell cell) {
        return gridRepository.save(cell);
    }
}
