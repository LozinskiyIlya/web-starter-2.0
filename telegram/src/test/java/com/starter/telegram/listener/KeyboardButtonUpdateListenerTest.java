package com.starter.telegram.listener;

import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.format.TextStyle;
import java.util.Locale;

import static com.starter.telegram.service.TelegramBotService.NEW_BILL_BUTTON;
import static com.starter.telegram.service.TelegramBotService.LATEST_BILLS;
import static java.time.ZoneOffset.UTC;

class KeyboardButtonUpdateListenerTest extends AbstractTelegramTest {

    @Autowired
    private KeyboardButtonUpdateListener listener;
    @Autowired
    private BillTestDataCreator billTestDataCreator;

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

    @Nested
    @DisplayName("On This Month")
    class OnThisMonth {

        @Test
        @DisplayName("renders no bills placeholder")
        void rendersPlaceholder() {
            // given
            final var currentMonth = Instant.now().atZone(UTC).toLocalDate().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate(LATEST_BILLS, chatId);
            billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(chatId);
                g.setOwner(userTestDataCreator.givenUserExists());
            });
            // when
            listener.processUpdate(update, bot);
            // then
            assertMessageSentToChatId(bot, chatId);
            assertSentMessageToChatIdContainsText(bot, chatId, "Nothing was tracked for the selected time range");
            assertSentMessageToChatIdContainsText(bot, chatId, currentMonth);
            assertSentMessageToChatIdContainsKeyboard(bot, chatId);
        }
    }
}