package com.starter.telegram.listener;

import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.starter.telegram.service.TelegramBotService.NEW_BILL_BUTTON;

class KeyboardButtonUpdateListenerTest extends AbstractTelegramTest {

    @Autowired
    private KeyboardButtonUpdateListener listener;

    @Nested
    @DisplayName("On New Bill")
    class OnNewBill {

        @Test
        @DisplayName("render new bill message")
        void renderNewBillMessage() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate(NEW_BILL_BUTTON, chatId);
            // when
            listener.processUpdate(update, bot);
            // then
            assertMessageSentToChatId(bot, chatId);
            assertSentMessageContainsText(bot, "Let the bot extract all important info");
        }
    }
}