package org.martynov.dev.repository;

import org.martynov.dev.entity.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {
    Optional<Swipe> findByTelegramIdAndSeriesId(Long telegramId, Long seriesId);
}
