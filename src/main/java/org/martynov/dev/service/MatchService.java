package org.martynov.dev.service;

import org.martynov.dev.entity.AppUser;
import org.martynov.dev.entity.Series;
import org.martynov.dev.entity.Swipe;
import org.martynov.dev.repository.SeriesRepository;
import org.martynov.dev.repository.SwipeRepository;
import org.martynov.dev.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    private final SwipeRepository swipeRepository;
    private final UserRepository userRepository;
    private final SeriesRepository seriesRepository;

    public MatchService(SwipeRepository swipeRepository,
                        UserRepository userRepository,
                        SeriesRepository seriesRepository) {
        this.swipeRepository = swipeRepository;
        this.userRepository = userRepository;
        this.seriesRepository = seriesRepository;
    }

    public record MatchResult(Long partnerId, Series series) {}

    @Transactional
    public Optional<MatchResult> processSwipe(Long telegramId, Long seriesId, boolean isLiked) {

        Optional<Swipe> existing = swipeRepository.findByTelegramIdAndSeriesId(telegramId, seriesId);
        Swipe swipe = existing.orElseGet(Swipe::new);
        swipe.setTelegramId(telegramId);
        swipe.setSeriesId(seriesId);
        swipe.setLiked(isLiked);
        swipeRepository.save(swipe);

        if (!isLiked) {
            return Optional.empty();
        }

        AppUser user = userRepository.findById(telegramId).orElse(null);
        if (user == null || user.getPartnerId() == null) {
            return Optional.empty();
        }

        Long partnerId = user.getPartnerId();
        Optional<Swipe> partnerSwipe = swipeRepository.findByTelegramIdAndSeriesId(partnerId, seriesId);

        if (partnerSwipe.isPresent() && partnerSwipe.get().isLiked()) {
            Series series = seriesRepository.findById(seriesId).orElse(null);
            if (series != null) {
                return Optional.of(new MatchResult(partnerId, series));
            }
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Series getNextSeries(Long telegramId) {
        return seriesRepository.findNextUnseenSeries(telegramId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Series> getMatchesList(Long telegramId) {
        AppUser user = userRepository.findById(telegramId).orElse(null);
        if (user == null || user.getPartnerId() == null) {
            return Collections.emptyList();
        }
        return seriesRepository.findMatchesForPair(telegramId, user.getPartnerId());
    }

    @Transactional(readOnly = true)
    public boolean hasPartner(Long telegramId) {
        return userRepository.findById(telegramId)
                .map(u -> u.getPartnerId() != null)
                .orElse(false);
    }
}