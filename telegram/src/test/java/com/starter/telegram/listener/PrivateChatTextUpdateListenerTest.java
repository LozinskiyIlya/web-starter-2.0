package com.starter.telegram.listener;

import com.pengrad.telegrambot.request.GetChat;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.AbstractTelegramTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
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
        @DisplayName("Updates userInfo")
        void updatesUserInfoOnTextMessage() {
            // given
            final var userChatId = random.nextLong();
            final var updatedBio = UUID.randomUUID().toString();
            final var existingUserInfo = userTestDataCreator.givenUserInfoExists(ui -> {
                ui.setTelegramChatId(userChatId);
                ui.setFirstName("John");
                ui.setLastName("Doe");
            });
            assertNull(existingUserInfo.getAvatar());
            final var update = mockGroupUpdate("some text", userChatId, userChatId);
            final var chatResponse = mockReturnedChatData(userChatId, updatedBio);
            when(bot.execute(Mockito.any(GetChat.class))).thenReturn(chatResponse);
            // when
            listener.processUpdate(update, bot);
            // then
            await().pollDelay(2, TimeUnit.SECONDS).until(() -> true); // info update is async
            final var updatedUserInfo = userTestDataCreator.userInfoRepository().findById(existingUserInfo.getId()).orElseThrow();
            assertNotEquals("John", updatedUserInfo.getFirstName());
            assertNotEquals("Doe", updatedUserInfo.getLastName());
            assertEquals(updatedBio, updatedUserInfo.getBio());
            assertNotNull(updatedUserInfo.getAvatar());
        }

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

    @Nested
    @DisplayName("On file message")
    class OnFileMessage {

        @Test
        @DisplayName("Send file received notice")
        void sendFileReceivedNotice() {
            // given
            final var userChatId = random.nextLong();
            final var update = mockGroupUpdateWithPhoto("some text", userChatId, userChatId);
            // when
            listener.processUpdate(update, bot);
            // then
            assertSentMessageToChatIdContainsText(bot, userChatId, "File received");
        }
    }
}