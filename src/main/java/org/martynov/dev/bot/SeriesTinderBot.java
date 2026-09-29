package org.martynov.dev.bot;

import org.martynov.dev.entity.AppUser;
import org.martynov.dev.entity.Series;
import org.martynov.dev.repository.UserRepository;
import org.martynov.dev.service.MatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class SeriesTinderBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final MatchService matchService;

    @Value("${TELEGRAM_BOT_USERNAME}")
    private String botUsername;

    public SeriesTinderBot(@Value("${TELEGRAM_BOT_TOKEN}") String botToken,
                           UserRepository userRepository,
                           @Lazy MatchService matchService) {
        super(botToken);
        this.userRepository = userRepository;
        this.matchService = matchService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackId = update.getCallbackQuery().getId();
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            answerCallback(callbackId);

            if ("show_matches".equals(data)) {
                matchService.sendMatchesList(chatId);
            } else if (data.startsWith("swipe_like_")) {
                Long seriesId = Long.parseLong(data.replace("swipe_like_", ""));
                matchService.processSwipe(chatId, seriesId, true);
                sendNextSeriesCard(chatId);
            } else if (data.startsWith("swipe_dislike_")) {
                Long seriesId = Long.parseLong(data.replace("swipe_dislike_", ""));
                matchService.processSwipe(chatId, seriesId, false);
                sendNextSeriesCard(chatId);
            }
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/start")) {
                handleStartCommand(chatId, messageText);
            } else if ("/matches".equals(messageText)) {
                matchService.sendMatchesList(chatId);
            } else if ("/next".equals(messageText)) {
                sendNextSeriesCard(chatId);
            }
        }
    }

    private void handleStartCommand(Long chatId, String messageText) {
        if (!userRepository.existsById(chatId)) {
            AppUser newUser = new AppUser();
            newUser.setTelegramId(chatId);
            userRepository.save(newUser);
        }

        String[] parts = messageText.split(" ");
        if (parts.length == 2) {
            try {
                Long partnerId = Long.parseLong(parts[1]);

                if (chatId.equals(partnerId)) {
                    sendMessage(chatId, "Нельзя привязать аккаунт сам к себе!");
                    return;
                }

                AppUser user = userRepository.findById(chatId).orElseThrow();
                user.setPartnerId(partnerId);
                userRepository.save(user);

                AppUser partner = userRepository.findById(partnerId).orElse(null);
                if (partner != null) {
                    partner.setPartnerId(chatId);
                    userRepository.save(partner);
                }

                sendMessage(chatId, "Отлично! Вы успешно привязаны к партнеру. Открывайте приложение и начинайте свайпать!");
                sendMessage(partnerId, "Партнер перешел по вашей ссылке! Теперь ваши совпадения общие.");

            } catch (NumberFormatException e) {
                sendMessage(chatId, "Ошибка: неверная ссылка.");
            }
        } else {
            String text = "Привет! Добро пожаловать в Series Tinder 🎬\n\n" +
                    "Свайпай карточки ниже или поделись ссылкой с партнером!\n" +
                    "Команды:\n" +
                    "/matches — список совпадений\n" +
                    "/next — следующая карточка";
            sendMessage(chatId, text);
        }

        sendNextSeriesCard(chatId);
    }

    public void sendNextSeriesCard(Long chatId) {
        Series series = matchService.getNextSeries(chatId);

        if (series == null) {
            sendMessage(chatId, "🎬 Вы просмотрели все доступные сериалы! Ожидайте пополнения базы.");
            return;
        }

        StringBuilder caption = new StringBuilder();
        caption.append("🍿 *").append(series.getTitle()).append("*\n\n");
        if (series.getRating() != null) {
            caption.append("⭐ Рейтинг: ").append(series.getRating()).append("\n");
        }
        if (series.getReleaseYear() != null) {
            caption.append("📅 Год: ").append(series.getReleaseYear()).append("\n");
        }
        if (series.getDescription() != null && !series.getDescription().isBlank()) {
            caption.append("\n").append(series.getDescription());
        }

        if (series.getPosterUrl() != null && !series.getPosterUrl().isBlank()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId.toString());
            photo.setPhoto(new InputFile(series.getPosterUrl()));
            photo.setCaption(caption.toString());
            photo.setParseMode("Markdown");
            photo.setReplyMarkup(createSwipeKeyboard(series.getId()));

            try {
                execute(photo);
            } catch (TelegramApiException e) {
                sendTextCard(chatId, caption.toString(), series.getId());
            }
        } else {
            sendTextCard(chatId, caption.toString(), series.getId());
        }
    }

    private void sendTextCard(Long chatId, String text, Long seriesId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(createSwipeKeyboard(seriesId));

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void answerCallback(String callbackId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public InlineKeyboardMarkup createSwipeKeyboard(Long seriesId) {
        InlineKeyboardButton dislikeBtn = InlineKeyboardButton.builder()
                .text("👎 Пропустить")
                .callbackData("swipe_dislike_" + seriesId)
                .build();

        InlineKeyboardButton likeBtn = InlineKeyboardButton.builder()
                .text("❤️ Хочу смотреть")
                .callbackData("swipe_like_" + seriesId)
                .build();

        InlineKeyboardButton matchesBtn = InlineKeyboardButton.builder()
                .text("🍿 Наши совпадения")
                .callbackData("show_matches")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(dislikeBtn, likeBtn))
                .keyboardRow(List.of(matchesBtn))
                .build();
    }
}