package com.starter.telegram.listener;

import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

            // when
            groupCommandListener.processUpdate(update, bot);

            // then
            assertTrue(((UserInfoRepository) userTestDataCreator.userInfoRepository()).findByTelegramChatId(senderChatId).isPresent());
            assertMessageSentToChatId(bot, owner.getTelegramChatId());
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