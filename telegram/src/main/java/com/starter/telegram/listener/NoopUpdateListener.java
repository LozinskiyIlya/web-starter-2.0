package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import org.springframework.stereotype.Component;

import static com.starter.telegram.service.TelegramBotService.latestKeyboard;
import static com.starter.telegram.service.render.TelegramStaticRenderer.withLatestKeyboard;


@Component
public class NoopUpdateListener implements UpdateListener {
    @Override
    public void processUpdate(Update update, final TelegramBot bot) {
        bot.execute(withLatestKeyboard(update.message().chat().id(),
                "Please use one of the available commands or buttons to interact with the bot.").replyMarkup(latestKeyboard()));
    }
}
