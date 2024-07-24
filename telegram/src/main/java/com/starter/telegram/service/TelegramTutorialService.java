package com.starter.telegram.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.*;
import com.pengrad.telegrambot.request.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramTutorialService {

    private static final String TUTORIAL_TEMPLATE = "<b>%s</b>\n\n%s";
    private static final List<TutorialStep> steps = List.of(
            new TutorialStep(
                    "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/Step1.gif",
                    "\uD83D\uDC4B Welcome to the AI Counting Bot!",
                    "Forward any payment-related text to this bot to get structured info"
            ),
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "Send PDFs or images \uD83E\uDDFE",
                    "The bot will extract the payment info"
            ),
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "Use <code>Groups</code> \uD83D\uDC65 to keep track of shared expenses",
                    "Each bill is attached to a group. When you send private message to the bot, the expense will be attached to the <b>\"Personal\"</b> group"
            ),
            new TutorialStep(
                    "https://media.giphy.com/media/JIX9t2j0ZTN9S/giphy.gif",
                    "\uD83C\uDF00 Check out other functions!",
                    "Add bills manually, view stats and insights, share bills with friends and much more!"
            )
    );

    public void onTutorialCommand(Update update, TelegramBot bot) {
        final var message = getNextStep(update.message().chat().id(), 0, null);
        bot.execute(message);
    }

    public void onStepChanged(Update update, TelegramBot bot) {
        final var query = update.callbackQuery();
        final var index = query.data().split("_")[2];
        final var step = Integer.parseInt(index);
        final var message = getNextStep(query.from().id(), step, query.maybeInaccessibleMessage());
        bot.execute(message);
    }

    public void onPinTutorial(Update update, TelegramBot bot) {
        final var chatId = update.callbackQuery().from().id();
        final var messageId = update.callbackQuery().maybeInaccessibleMessage().messageId();
        final var pinRequest = new PinChatMessage(chatId, messageId);
        bot.execute(pinRequest);
    }

    private BaseRequest<?, ?> getNextStep(Long chatId, int step, MaybeInaccessibleMessage message) {
        final var nextStep = steps.get(step);
        final var buttons = new LinkedList<InlineKeyboardButton>();
        if (step > 0) {
            buttons.add(new InlineKeyboardButton("< Prev").callbackData(TUTORIAL_PREV_PREFIX + (step - 1)));
        }
        if (step < steps.size() - 1) {
            buttons.add(new InlineKeyboardButton("Next >").callbackData(TUTORIAL_NEXT_PREFIX + (step + 1)));
        } else {
            buttons.add(new InlineKeyboardButton("Pin \uD83D\uDCCC").callbackData(TUTORIAL_PIN_PREFIX));
        }
        final var keyboard = new InlineKeyboardMarkup(buttons.toArray(new InlineKeyboardButton[0]));
        final var caption = TUTORIAL_TEMPLATE.formatted(nextStep.getTitle(), nextStep.getCaption());

        if (message instanceof Message && message.messageId() != null) {
            return new EditMessageMedia(chatId, message.messageId(),
                    new InputMediaAnimation(nextStep.getGifPath())
                            .caption(caption)
                            .parseMode(ParseMode.HTML))
                    .replyMarkup(keyboard);
        }

        return new SendAnimation(chatId, nextStep.getGifPath())
                .caption(caption)
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboard);
    }


    @Data
    @AllArgsConstructor
    private static class TutorialStep {
        private String gifPath;
        private String title;
        private String caption;
    }
}
