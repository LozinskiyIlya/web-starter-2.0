package com.starter.telegram.listener;

import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.starter.telegram.service.TelegramBotService.LATEST_BILLS;
import static com.starter.telegram.service.TelegramBotService.NEW_BILL_BUTTON;

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
    @DisplayName("On Latest Bills")
    class onLatestBills {


        @Test
        @DisplayName("Sends latest bills")
        void sendsLatestBills() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate(LATEST_BILLS, chatId);
            final var personal = billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(chatId);
                g.setOwner(userTestDataCreator.givenUserExists());
            });
            billTestDataCreator.givenBillExists(b -> {
                b.setGroup(personal);
                b.setPurpose("Dinner");
            });
            billTestDataCreator.givenBillExists(b -> {
                b.setGroup(personal);
                b.setPurpose("Rent");
            });
            // when
            listener.processUpdate(update, bot);
            // then
            assertSentMessageToChatIdContainsText(bot, chatId, "Dinner");
            assertSentMessageToChatIdContainsText(bot, chatId, "Rent");
        }
    }
}