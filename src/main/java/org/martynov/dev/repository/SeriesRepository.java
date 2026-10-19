package org.martynov.dev.repository;

import org.martynov.dev.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {

    boolean existsByTitle(String title);

    @Query(value = """
            SELECT s.* FROM series s
            WHERE NOT EXISTS (
                SELECT 1 FROM swipes sw
                WHERE sw.series_id = s.id 
                  AND sw.telegram_id = :telegramId
            )
            ORDER BY s.rating DESC NULLS LAST, s.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Series> findNextUnseenSeries(@Param("telegramId") Long telegramId);

    @Query(value = """
            SELECT s.* FROM series s
            JOIN swipes sw1 ON sw1.series_id = s.id AND sw1.telegram_id = :user1Id AND sw1.is_liked = true
            JOIN swipes sw2 ON sw2.series_id = s.id AND sw2.telegram_id = :user2Id AND sw2.is_liked = true
            ORDER BY s.rating DESC NULLS LAST
            """, nativeQuery = true)
    List<Series> findMatchesForPair(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}