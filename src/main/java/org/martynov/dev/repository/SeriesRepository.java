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

    @Query(value = "SELECT * FROM series s WHERE s.id NOT IN (SELECT series_id FROM swipes WHERE telegram_id = :userId) LIMIT 1", nativeQuery = true)
    Optional<Series> findNextUnseenSeries(@Param("userId") Long userId);

    boolean existsByTitle(String title);

    @Query("""
        SELECT s FROM Series s
        WHERE s.id IN (
            SELECT sw1.seriesId FROM Swipe sw1 
            WHERE sw1.telegramId = :user1Id AND sw1.isLiked = true
        )
        AND s.id IN (
            SELECT sw2.seriesId FROM Swipe sw2 
            WHERE sw2.telegramId = :user2Id AND sw2.isLiked = true
        )
        ORDER BY s.id DESC
    """)
    List<Series> findMatchesForPair(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}