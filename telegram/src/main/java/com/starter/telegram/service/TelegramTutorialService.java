package com.starter.telegram.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendAnimation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

import static com.starter.telegram.listener.CallbackQueryUpdateListener.TUTORIAL_NEXT_PREFIX;
import static com.starter.telegram.listener.CallbackQueryUpdateListener.TUTORIAL_PREV_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramTutorialService {

    private static final String TUTORIAL_TEMPLATE = "<b>%s</b>\n\n%s";
    private static final List<TutorialStep> steps = List.of(
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "Welcome to the AI Counting Bot!",
                    "Forward any payment-related text to this bot to get structured info"
            ),
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "Send PDFs or images",
                    "The bot will extract the payment info"
            ),
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "Use groups to keep track of shared expenses",
                    "Each bill is attached to a group. When you send private message to the bot, the expense will be attached to the <b>\"Personal\"</b> group"
            ),
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "Check out other functions!",
                    "Add bills manually, view stats and insights, share bills with friends and much more!"
            )
    );

    public void onTutorialCommand(Update update, TelegramBot bot) {
        final var message = getNextStep(update.message().chat().id(), 0);
        bot.execute(message);
    }

    public void onStepChanged(Update update, TelegramBot bot) {
        final var data = update.callbackQuery().data().split("_");
        final var step = Integer.parseInt(data[2]);
        final var message = getNextStep(update.callbackQuery().from().id(), step);
        bot.execute(message);
    }

    private SendAnimation getNextStep(Long chatId, int step) {
        final var nextStep = steps.get(step);
        final var buttons = new LinkedList<InlineKeyboardButton>();
        if (step > 0) {
            buttons.add(new InlineKeyboardButton("< Prev").callbackData(TUTORIAL_PREV_PREFIX + (step - 1)));
        }
        if (step < steps.size() - 1) {
            buttons.add(new InlineKeyboardButton("Next >").callbackData(TUTORIAL_NEXT_PREFIX + (step + 1)));
        }
        return new SendAnimation(chatId, nextStep.getGifPath())
                .caption(TUTORIAL_TEMPLATE.formatted(nextStep.getTitle(), nextStep.getCaption()))
                .replyMarkup(new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0])))
                .parseMode(ParseMode.HTML);
    }


    @Data
    @AllArgsConstructor
    private static class TutorialStep {
        private String gifPath;
        private String title;
        private String caption;
    }
}
