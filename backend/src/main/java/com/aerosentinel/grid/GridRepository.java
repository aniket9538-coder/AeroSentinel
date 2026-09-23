package com.aerosentinel.grid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GridRepository extends JpaRepository<GridCell, UUID> {
    Optional<GridCell> findByH3Index(String h3Index);
    List<GridCell> findByCityId(UUID cityId);
}
