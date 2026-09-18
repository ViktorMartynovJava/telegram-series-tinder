package org.martynov.dev.bot;

import org.martynov.dev.entity.AppUser;
import org.martynov.dev.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class SeriesTinderBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;

    @Value("${TELEGRAM_BOT_USERNAME}")
    private String botUsername;

    public SeriesTinderBot(@Value("${TELEGRAM_BOT_TOKEN}") String botToken, UserRepository userRepository) {
        super(botToken);
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("/start")) {
                handleStartCommand(chatId, messageText);
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
                    "Запускай Mini App по кнопке меню или поделись ссылкой с партнером прямо из приложения!";
            sendMessage(chatId, text);
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}