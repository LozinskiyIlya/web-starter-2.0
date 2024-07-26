package com.starter.telegram.listener;

import com.starter.domain.entity.Group;
import com.starter.domain.repository.UserSettingsRepository;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.starter.telegram.service.TelegramBotService.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyboardButtonUpdateListenerTest extends AbstractTelegramTest {

    @Autowired
    private KeyboardButtonUpdateListener listener;
    @Autowired
    private BillTestDataCreator billTestDataCreator;
    @Autowired
    private UserSettingsRepository userSettingsRepository;

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
        @DisplayName("send latest bills")
        void sendLatestBills() {
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

    @Nested
    @DisplayName("On My Groups")
    class OnMyGroups {

        @Test
        @DisplayName("sends user's groups")
        void sendsLatestBills() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate(GROUPS, chatId);
            final var owner = userTestDataCreator.givenUserInfoExists(ui ->
                    ui.setTelegramChatId(chatId)).getUser();
            final var personal = billTestDataCreator.givenGroupExists(g -> {
                g.setTitle(Group.PERSONAL);
                g.setOwner(owner);
            });
            final var member = userTestDataCreator.givenUserInfoExists(ui -> {
            }).getUser();
            final var other = billTestDataCreator.givenGroupExists(g -> {
                g.setOwner(owner);
                g.setMembers(List.of(owner, member));
                g.setTitle("Other");
            });
            billTestDataCreator.givenBillExists(b -> b.setGroup(personal));
            billTestDataCreator.givenBillExists(b -> b.setGroup(personal));
            billTestDataCreator.givenBillExists(b -> b.setGroup(other));
            // when
            listener.processUpdate(update, bot);
            // then
            assertSentMessageToChatIdContainsText(bot, chatId, Group.PERSONAL);
            assertSentMessageToChatIdContainsText(bot, chatId, "Other");
            assertSentMessageToChatIdContainsText(bot, chatId, "2 bills • 1 members");
            assertSentMessageToChatIdContainsText(bot, chatId, "1 bills • 2 members");
        }
    }

    @Nested
    @DisplayName("On Settings")
    class OnSettings {

        @Test
        @DisplayName("send settings message")
        void sendSettingsMessage() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate(SETTINGS, chatId);
            final var user = userTestDataCreator.givenUserInfoExists(ui ->
                            ui.setTelegramChatId(chatId))
                    .getUser();
            userTestDataCreator.givenUserSettingsExists(s -> s.setUser(user));
            // when
            listener.processUpdate(update, bot);
            // then
            assertSentMessageToChatIdContainsText(bot, chatId, "settings");
            assertSentMessageToChatIdContainsKeyboard(bot, chatId);
        }

        @Test
        @DisplayName("create settings if missing")
        void createSettingsIfMissing() {
            // given
            final var chatId = random.nextLong();
            final var update = mockCommandUpdate(SETTINGS, chatId);
            final var user = userTestDataCreator.givenUserInfoExists(ui ->
                            ui.setTelegramChatId(chatId))
                    .getUser();
            assertTrue(userSettingsRepository.findOneByUser(user).isEmpty());
            // when
            listener.processUpdate(update, bot);
            // then
            assertTrue(userSettingsRepository.findOneByUser(user).isPresent());
        }
    }
}