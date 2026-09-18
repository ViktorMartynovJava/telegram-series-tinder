package org.martynov.dev.repository;

import org.martynov.dev.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    @Query(value = "SELECT * FROM series s WHERE s.id NOT IN (SELECT series_id FROM swipes WHERE telegram_id = :userId) LIMIT 1", nativeQuery = true)
    Optional<Series> findNextUnseenSeries(@Param("userId") Long userId);
}
