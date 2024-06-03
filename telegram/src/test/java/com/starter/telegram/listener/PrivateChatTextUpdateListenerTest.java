package com.starter.telegram.listener;

import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.AbstractTelegramTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrivateChatTextUpdateListenerTest extends AbstractTelegramTest {

    @Autowired
    private PrivateChatTextUpdateListener listener;

    @SpyBean
    private GroupRepository groupRepository;

    @Nested
    @DisplayName("On text message")
    class OnTextMessage {

        @Test
        @Transactional
        @DisplayName("creates Personal group if not exists")
        void createPersonalGroupIfNotExist() {
            // given
            final var userChatId = random.nextLong();
            final var user = userTestDataCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(userChatId)).getUser();
            final var update = mockGroupUpdate("some text", userChatId, userChatId);
            // when
            listener.processUpdate(update, bot);
            // then
            final var group = groupRepository.findByChatId(userChatId).orElseThrow();
            assertTrue(group.contains(user));
            assertEquals(userChatId, group.getChatId());
            assertEquals("Personal", group.getTitle());
            assertEquals(user.getId(), group.getOwner().getId());
        }

        @Test
        @DisplayName("return existing group without creating a new one")
        void returnExistingGroupWithoutCreatingNewOne() {
            // given
            final long existingChatId = random.nextLong();
            final String existingTitle = "Personal";
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(existingChatId));
            billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(existingChatId);
                g.setTitle(existingTitle);
                g.setOwner(owner.getUser());
            });

            final var update = mockGroupUpdate(null, existingChatId, existingChatId);
            final var message = update.message();
            when(message.chat().id()).thenReturn(existingChatId);
            when(message.chat().title()).thenReturn(existingTitle);

            // when
            listener.processUpdate(update, bot);

            // then
            final var group = groupRepository.findByChatId(existingChatId);
            assertTrue(group.isPresent());
            assertEquals(existingTitle, group.get().getTitle());
            assertEquals(owner.getUser().getId(), group.get().getOwner().getId());
            verify(groupRepository, never()).save(any(Group.class));  // Ensure no new group is created
        }
    }
}