package org.martynov.dev.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {


    public static InlineKeyboardMarkup createSwipeKeyboard(Long seriesId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton dislikeBtn = new InlineKeyboardButton();
        dislikeBtn.setText("👎 Пропустить");
        dislikeBtn.setCallbackData("DISLIKE:" + seriesId);

        InlineKeyboardButton likeBtn = new InlineKeyboardButton();
        likeBtn.setText("❤️ Хочу смотреть");
        likeBtn.setCallbackData("LIKE:" + seriesId);

        List<InlineKeyboardButton> row1 = List.of(dislikeBtn, likeBtn);

        InlineKeyboardButton matchesBtn = new InlineKeyboardButton();
        matchesBtn.setText("🍿 Наши совпадения");
        matchesBtn.setCallbackData("SHOW_MATCHES");

        List<InlineKeyboardButton> row2 = List.of(matchesBtn);

        markup.setKeyboard(List.of(row1, row2));
        return markup;
    }


    public static ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setIsPersistent(true); // Закрепляет клавиатуру, чтобы Telegram не прятал её
        markup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🍿 Наши совпадения"));
        row1.add(new KeyboardButton("🎬 Следующий сериал"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🔗 Пригласить партнёра"));

        keyboard.add(row1);
        keyboard.add(row2);

        markup.setKeyboard(keyboard);
        return markup;
    }
}