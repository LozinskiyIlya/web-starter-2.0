package com.starter.telegram.listener;

import com.pengrad.telegrambot.request.SendMessage;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GroupCommandListenerTest extends AbstractUpdateListenerTest {

    @Autowired
    private UserTestDataCreator userTestDataCreator;

    @Autowired
    private BillTestDataCreator billTestDataCreator;

    @Autowired
    private GroupCommandListener groupCommandListener;


    @DisplayName("On /addme command")
    abstract class OnAddmeCommand {

        protected Supplier<UserInfo> senderInfoSupplier;

        @Test
        @DisplayName("sends request to the owner")
        void sendsRequestToOwner() {
            // given
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(random.nextLong()));
            final var sender = senderInfoSupplier.get();
            final var senderChatId = sender.getTelegramChatId();
            final var groupChatId = random.nextLong();
            billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(groupChatId);
                g.setOwner(owner.getUser());
            });
            final var update = mockGroupUpdate("/addme", senderChatId, groupChatId);
            final var bot = mockBot();

            // when
            groupCommandListener.processUpdate(update, bot);

            //then
            final var captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot, times(1)).execute(captor.capture());
            final var actualRequest = captor.getValue();
            final var sendTo = actualRequest.getParameters().get("chat_id").toString();
            assertTrue(sendTo.contains(owner.getTelegramChatId().toString()));
        }
    }

    @Nested
    @DisplayName("As existing user")
    class AsExistingUser extends OnAddmeCommand {
        {
            senderInfoSupplier = () -> userTestDataCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(random.nextLong()));
        }
    }

    @Nested
    @DisplayName("As new user")
    class AsNewUser extends OnAddmeCommand {
        {
            senderInfoSupplier = () -> {
                final var someUserInfo = new UserInfo();
                someUserInfo.setTelegramChatId(random.nextLong());
                return someUserInfo;
            };
        }
    }
}