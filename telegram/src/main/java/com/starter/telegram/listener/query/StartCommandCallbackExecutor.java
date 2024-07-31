package com.starter.telegram.listener.query;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.starter.telegram.service.TelegramStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.starter.telegram.service.TelegramStateMachine.State.SET_CURRENCY;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderCurrencyExpectedMessage;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderRecognizeMyBill;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartCommandCallbackExecutor implements CallbackExecutor {

    private static final String PREFIX = "start_";
    public static final String SET_CURRENCY_PREFIX = PREFIX + "set_currency";
    public static final String POST_BILL_PREFIX = PREFIX + "post_bill";

    private final TelegramStateMachine stateMachine;

    @Override
    public void execute(TelegramBot bot, CallbackQuery query, Long chatId) {
        final var callbackData = query.data();
        if (callbackData.startsWith(SET_CURRENCY_PREFIX)) {
            final var message = renderCurrencyExpectedMessage(chatId);
            bot.execute(message);
            stateMachine.setState(chatId, SET_CURRENCY);
        } else if (callbackData.startsWith(POST_BILL_PREFIX)) {
            final var message = renderRecognizeMyBill(chatId);
            bot.execute(message);
            stateMachine.removeState(chatId);
        }
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
