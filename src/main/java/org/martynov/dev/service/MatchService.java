package org.martynov.dev.service;

import org.martynov.dev.bot.SeriesTinderBot;
import org.martynov.dev.entity.AppUser;
import org.martynov.dev.entity.Series;
import org.martynov.dev.entity.Swipe;
import org.martynov.dev.repository.SeriesRepository;
import org.martynov.dev.repository.SwipeRepository;
import org.martynov.dev.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
                        @Lazy SeriesTinderBot bot) {
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

        if (!isLiked) {
            return false;
        }

        AppUser user = userRepository.findById(telegramId).orElse(null);
        if (user == null || user.getPartnerId() == null) {
            return false;
        }

        Optional<Swipe> partnerSwipe = swipeRepository.findByTelegramIdAndSeriesId(user.getPartnerId(), seriesId);

        if (partnerSwipe.isPresent() && partnerSwipe.get().isLiked()) {
            Series series = seriesRepository.findById(seriesId).orElseThrow();

            String matchText = "🔥 У вас совпадение!\n\n" +
                    "Вы оба хотите посмотреть: *" + series.getTitle() + "*\n" +
                    (series.getWatchUrl() != null && !series.getWatchUrl().isBlank()
                            ? "🔗 Ссылка: " + series.getWatchUrl()
                            : "");

            bot.sendMessage(telegramId, matchText);
            bot.sendMessage(user.getPartnerId(), matchText);

            return true;
        }
        return false;
    }

    public Series getNextSeries(Long telegramId) {
        return seriesRepository.findNextUnseenSeries(telegramId).orElse(null);
    }

    @Transactional(readOnly = true)
    public void sendMatchesList(Long telegramId) {
        AppUser user = userRepository.findById(telegramId).orElse(null);
        if (user == null || user.getPartnerId() == null) {
            bot.sendMessage(telegramId, "⚠️ У вас пока не привязан партнёр.");
            return;
        }

        List<Series> matches = seriesRepository.findMatchesForPair(telegramId, user.getPartnerId());

        if (matches.isEmpty()) {
            bot.sendMessage(telegramId, "У вас с партнёром пока нет общих совпадений. Продолжайте свайпать! 🎬");
            return;
        }

        StringBuilder sb = new StringBuilder("🍿 *Ваши общие совпадения:*\n\n");
        for (int i = 0; i < matches.size(); i++) {
            Series s = matches.get(i);
            sb.append(i + 1).append(". *").append(s.getTitle()).append("*\n");
            if (s.getRating() != null) {
                sb.append("   ⭐ Рейтинг: ").append(s.getRating()).append("\n");
            }
            if (s.getWatchUrl() != null && !s.getWatchUrl().isBlank()) {
                sb.append("   🔗 ").append(s.getWatchUrl()).append("\n");
            }
            sb.append("\n");
        }

        bot.sendMessage(telegramId, sb.toString());
    }
}