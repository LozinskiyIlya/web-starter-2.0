package com.starter.telegram.service.listener;

import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupUpdateListenerTest extends AbstractUpdateListenerTest {

    @Autowired
    private GroupUpdateListener listener;

    @Autowired
    private UserTestDataCreator userTestDataCreator;

    @Autowired
    private GroupRepository groupRepository;

    @Nested
    @DisplayName("On bot added")
    class OnBotAdded {
        @Test
        @DisplayName("create a group if not exists")
        void createGroupIfNotExist() {
            // given
            final var userChatId = random.nextLong();
            final var user = userTestDataCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(userChatId)).getUser();
            final var message = "I owe you $100";
            final var update = mockGroupUpdate(message, userChatId);
            // when
            listener.processUpdate(update, mockBot());
            // then
            final var group = groupRepository.findByChatId(update.message().chat().id());
            assertTrue(group.isPresent());
            assertEquals(user.getId(), group.get().getOwner().getId());
            assertEquals(update.message().chat().title(), group.get().getTitle());
        }
    }


    @Nested
    @DisplayName("On text message")
    class OnTextMessage {
    }

    @Nested
    @DisplayName("On file message")
    class OnFileMessage {
    }
}