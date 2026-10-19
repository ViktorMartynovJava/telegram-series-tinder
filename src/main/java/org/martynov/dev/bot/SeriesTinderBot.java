package org.martynov.dev.bot;

import lombok.extern.slf4j.Slf4j;
import org.martynov.dev.entity.AppUser;
import org.martynov.dev.entity.Series;
import org.martynov.dev.repository.UserRepository;
import org.martynov.dev.service.MatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SeriesTinderBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final MatchService matchService;
    private final UserRepository userRepository;

    public SeriesTinderBot(@Value("${telegram.bot.token}") String botToken,
                           @Value("${telegram.bot.username}") String botUsername,
                           MatchService matchService,
                           UserRepository userRepository) {
        super(botToken);
        this.botUsername = botUsername;
        this.matchService = matchService;
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке update: ", e);
        }
    }

    private void handleTextMessage(Update update) {
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();

        if (text.startsWith("/start")) {
            handleStartCommand(chatId, text);
            return;
        }

        switch (text) {
            case "🎬 Следующий сериал" -> {
                Series next = matchService.getNextSeries(chatId);
                sendSeriesCard(chatId, next);
            }
            case "🍿 Наши совпадения" -> sendMatchesList(chatId);
            case "🔗 Пригласить партнёра" -> {
                String inviteLink = "https://t.me/" + getBotUsername() + "?start=" + chatId;
                sendMessage(chatId, "Отправь эту ссылку своей половинке:\n" + inviteLink);
            }
            default -> sendMessage(chatId, "Используй кнопки меню ниже 👇");
        }
    }

    private void handleCallbackQuery(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if ("SHOW_MATCHES".equals(data)) {
            sendMatchesList(chatId);
            return;
        }

        if (!data.contains(":")) {
            return;
        }

        String[] parts = data.split(":");
        String action = parts[0];
        Long seriesId = Long.parseLong(parts[1]);

        boolean isLiked = "LIKE".equals(action);

        Optional<MatchService.MatchResult> matchOpt = matchService.processSwipe(chatId, seriesId, isLiked);

        matchOpt.ifPresent(match -> {
            String matchText = "🔥 *У вас совпадение!*\n\n" +
                    "Вы оба хотите посмотреть: *" + match.series().getTitle() + "* 🍿\n" +
                    (match.series().getWatchUrl() != null && !match.series().getWatchUrl().isBlank()
                            ? "🔗 Ссылка: " + match.series().getWatchUrl()
                            : "");

            sendMessage(chatId, matchText);
            sendMessage(match.partnerId(), matchText);
        });

        Series next = matchService.getNextSeries(chatId);
        sendSeriesCard(chatId, next);
    }

    private void handleStartCommand(Long chatId, String text) {
        AppUser user = userRepository.findById(chatId).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setTelegramId(chatId);
            return newUser;
        });

        String[] parts = text.split(" ");
        if (parts.length > 1) {
            try {
                Long potentialPartnerId = Long.parseLong(parts[1]);
                if (!potentialPartnerId.equals(chatId)) {
                    user.setPartnerId(potentialPartnerId);

                    userRepository.findById(potentialPartnerId).ifPresent(partner -> {
                        partner.setPartnerId(chatId);
                        userRepository.save(partner);
                        sendMessage(potentialPartnerId, "🎉 К вам успешно привязался партнёр!");
                    });
                }
            } catch (NumberFormatException ignored) {
            }
        }

        userRepository.save(user);

        String welcomeText = "Привет! Это бот для совместного выбора сериалов 🎬\n\n" +
                "Свайпай карточки: если вы оба с партнёром нажмёте «❤️ Хочу смотреть», бот сразу пришлёт уведомление!\n\n" +
                (user.getPartnerId() == null
                        ? "⚠️ У тебя ещё не привязан партнёр. Нажми «🔗 Пригласить партнёра», чтобы отправить ссылку."
                        : "✅ Партнёр подключен. Приятного выбора!");

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(welcomeText);
        message.setReplyMarkup(KeyboardFactory.createMainMenuKeyboard());
        executeSafe(message);

        Series first = matchService.getNextSeries(chatId);
        sendSeriesCard(chatId, first);
    }

    public void sendSeriesCard(Long chatId, Series series) {
        if (series == null) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("🎉 Ты просмотрел все доступные сериалы! Жди обновлений или проверь раздел «🍿 Наши совпадения».");
            message.setReplyMarkup(KeyboardFactory.createMainMenuKeyboard());
            executeSafe(message);
            return;
        }

        String desc = series.getDescription() != null ? series.getDescription() : "";
        if (desc.length() > 600) {
            desc = desc.substring(0, 597) + "...";
        }

        String caption = String.format(
                "🎬 *%s* (%s)\n" +
                        "⭐ Рейтинг: *%s*\n\n" +
                        "%s",
                escapeMarkdown(series.getTitle()),
                series.getReleaseYear() != null ? series.getReleaseYear() : "—",
                series.getRating() != null ? series.getRating() : "—",
                desc
        );

        if (series.getPosterUrl() != null && !series.getPosterUrl().isBlank()) {
            try {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(chatId.toString());
                photo.setPhoto(new InputFile(series.getPosterUrl().trim()));
                photo.setCaption(caption);
                photo.setParseMode("Markdown");
                photo.setReplyMarkup(KeyboardFactory.createSwipeKeyboard(series.getId()));
                execute(photo);
                return;
            } catch (TelegramApiException e) {
                log.warn("Не удалось отправить фото по URL: {}. Ошибка: {}. Отправляю текстом.",
                        series.getPosterUrl(), e.getMessage());
            }
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(caption);
        message.setParseMode("Markdown");
        message.setReplyMarkup(KeyboardFactory.createSwipeKeyboard(series.getId()));
        executeSafe(message);
    }

    private void sendMatchesList(Long chatId) {
        if (!matchService.hasPartner(chatId)) {
            sendMessage(chatId, "У вас пока не привязан партнёр. Нажмите «🔗 Пригласить партнёра».");
            return;
        }

        List<Series> matches = matchService.getMatchesList(chatId);

        if (matches.isEmpty()) {
            sendMessage(chatId, "У вас с партнёром пока нет общих совпадений. Продолжайте свайпать! 🎬");
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

        sendMessage(chatId, sb.toString());
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(KeyboardFactory.createMainMenuKeyboard());
        executeSafe(message);
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }

    private void executeSafe(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке SendMessage в chat {}: {}", message.getChatId(), e.getMessage());
        }
    }
}