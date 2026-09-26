package com.aerosentinel.grid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GridRepository extends JpaRepository<GridCell, UUID> {

    Optional<GridCell> findByH3Index(String h3Index);

    boolean existsByH3Index(String h3Index);

    List<GridCell> findByCityId(UUID cityId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE grid_cells SET boundary = ST_GeomFromText(:wkt, 4326)::geography WHERE h3_index = :h3Index", nativeQuery = true)
    int updateBoundary(@Param("h3Index") String h3Index, @Param("wkt") String wkt);

    @Query(value = "SELECT ST_AsText(boundary) FROM grid_cells WHERE h3_index = :h3Index", nativeQuery = true)
    Optional<String> findBoundaryWktByH3Index(@Param("h3Index") String h3Index);

    @Query(value = "SELECT ST_IsValid(boundary::geometry) FROM grid_cells WHERE h3_index = :h3Index", nativeQuery = true)
    Boolean isBoundaryValid(@Param("h3Index") String h3Index);

    @Query(value = "SELECT COUNT(*) FROM grid_cells WHERE boundary IS NOT NULL", nativeQuery = true)
    long countCellsWithNonNullBoundary();
}
