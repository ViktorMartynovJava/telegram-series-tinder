package org.martynov.dev.service;

import org.martynov.dev.bot.SeriesTinderBot; // <-- Импортируем бота
import org.martynov.dev.entity.AppUser;
import org.martynov.dev.entity.Series;
import org.martynov.dev.entity.Swipe;
import org.martynov.dev.repository.SeriesRepository;
import org.martynov.dev.repository.SwipeRepository;
import org.martynov.dev.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MatchService {

    private final SwipeRepository swipeRepository;
    private final UserRepository userRepository;
    private final SeriesRepository seriesRepository;
    private final SeriesTinderBot bot;

    public MatchService(SwipeRepository swipeRepository,
                        UserRepository userRepository,
                        SeriesRepository seriesRepository,
                        SeriesTinderBot bot) {
        this.swipeRepository = swipeRepository;
        this.userRepository = userRepository;
        this.seriesRepository = seriesRepository;
        this.bot = bot;
    }

    @Transactional
    public boolean processSwipe(Long telegramId, Long seriesId, boolean isLiked) {
        Swipe swipe = new Swipe();
        swipe.setTelegramId(telegramId);
        swipe.setSeriesId(seriesId);
        swipe.setLiked(isLiked);
        swipeRepository.save(swipe);

        if (!isLiked) return false;

        AppUser user = userRepository.findById(telegramId).orElse(null);
        if (user == null || user.getPartnerId() == null) return false;

        Optional<Swipe> partnerSwipe = swipeRepository.findByTelegramIdAndSeriesId(user.getPartnerId(), seriesId);

        if (partnerSwipe.isPresent() && partnerSwipe.get().isLiked()) {
            Series series = seriesRepository.findById(seriesId).orElseThrow();

            String matchText = "🔥 У вас совпадение!\n" +
                    "Вы оба хотите посмотреть: " + series.getTitle() + "\n" +
                    "Ссылка: " + series.getWatchUrl();

            bot.sendMessage(telegramId, matchText);
            bot.sendMessage(user.getPartnerId(), matchText);

            return true;
        }
        return false;
    }

    public Series getNextSeries(Long telegramId) {
        return seriesRepository.findNextUnseenSeries(telegramId).orElse(null);
    }
}