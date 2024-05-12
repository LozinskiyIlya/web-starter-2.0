package com.starter.telegram.listener;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.AbstractTelegramTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupUpdateListenerTest extends AbstractTelegramTest {

    @Autowired
    private GroupUpdateListener listener;

    @SpyBean
    private GroupRepository groupRepository;

    @Nested
    @DisplayName("On bot added")
    class OnBotAdded {

        @Test
        @Transactional
        @DisplayName("create a group if not exists")
        void createGroupIfNotExist() {
            // given
            final var userChatId = random.nextLong();
            final var groupChatId = random.nextLong();
            final var user = userTestDataCreator.givenUserInfoExists(ui -> ui.setTelegramChatId(userChatId)).getUser();
            final var update = mockGroupUpdate(null, userChatId, groupChatId);
            final var message = update.message();
            final var mockedNewUsers = new User[]{mockBotUser()};
            when(message.newChatMembers()).thenReturn(mockedNewUsers);
            // when
            listener.processUpdate(update, bot);
            // then
            final var group = groupRepository.findByChatId(update.message().chat().id()).orElseThrow();
            assertTrue(group.contains(user));
            Assertions.assertEquals(groupChatId, group.getChatId());
            assertEquals(user.getId(), group.getOwner().getId());
            assertEquals(update.message().chat().title(), group.getTitle());
        }

        @Test
        @DisplayName("return existing group without creating a new one")
        void returnExistingGroup() {
            // given
            final long existingChatId = random.nextLong();
            final String existingTitle = "Existing Group";
            billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(existingChatId);
                g.setTitle(existingTitle);
            });

            final var update = mockGroupUpdate(null, null, existingChatId);
            final var message = update.message();
            when(message.chat().id()).thenReturn(existingChatId);
            when(message.chat().title()).thenReturn(existingTitle);
            when(message.newChatMembers()).thenReturn(null); // No new members

            // when
            listener.processUpdate(update, bot);

            // then
            final var group = groupRepository.findByChatId(existingChatId);
            assertTrue(group.isPresent());
            assertEquals(existingTitle, group.get().getTitle());
            verify(groupRepository, never()).save(any(Group.class));  // Ensure no new group is created
        }

        @Test
        @DisplayName("handle non-existence of group when bot not added")
        void handleNonExistenceWhenBotNotAdded() {
            // given
            final long nonExistingChatId = random.nextLong();
            final var update = mockGroupUpdate(null, null, nonExistingChatId);  // No new members
            final var message = update.message();
            when(message.chat().id()).thenReturn(nonExistingChatId);
            when(message.newChatMembers()).thenReturn(new User[]{});  // Bot is not included
            // when
            Executable action = () -> listener.processUpdate(update, bot);
            // then
            assertThrows(NoSuchElementException.class, action); // Expecting to throw, as no group should be created/found
            assertFalse(groupRepository.findByChatId(nonExistingChatId).isPresent());
            verify(groupRepository, never()).save(any(Group.class));  // No group should be saved
        }

        @Test
        @DisplayName("bot is not the new member")
        void botNotTheNewMember() {
            // given
            final var existingChatId = random.nextLong();
            billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(existingChatId);
                g.setTitle("Existing Group");
            });
            final var update = mockGroupUpdate(null, null, existingChatId);
            final var message = update.message();
            final var mockedUser = mock(User.class);
            when(mockedUser.username()).thenReturn(null);
            when(message.newChatMembers()).thenReturn(new User[]{mockedUser});
            // when
            listener.processUpdate(update, bot);
            // then
            verify(groupRepository, never()).save(any(Group.class));
        }

        @Test
        @DisplayName("new Chat Members with Null Username")
        void newChatMembersWithNullUsername() {
            // given
            final var existingChatId = random.nextLong();
            billTestDataCreator.givenGroupExists(g -> {
                g.setChatId(existingChatId);
                g.setTitle("Existing Group");
            });
            final var update = mockGroupUpdate(null, null, existingChatId);
            final var message = update.message();
            final var mockedUser = mock(User.class);
            when(mockedUser.username()).thenReturn(null);
            when(message.newChatMembers()).thenReturn(new User[]{mockedUser});
            // when
            Executable action = () -> listener.processUpdate(update, bot);
            // then
            assertDoesNotThrow(action);
        }

        @Test
        @DisplayName("missing Group ID in Update")
        void missingGroupIdInUpdate() {
            // given
            final var update = mock(Update.class);
            final var message = mock(Message.class);
            when(update.message()).thenReturn(message);
            when(message.chat()).thenReturn(mock(Chat.class)); // Chat mock without ID
            // when
            Executable action = () -> listener.processUpdate(update, bot);
            // then
            assertThrows(NoSuchElementException.class, action); // Assuming your method throws when chat ID is missing
        }
    }

    @Nested
    @DisplayName("On id changed")
    class OnIdChanged {

        @Test
        @DisplayName("update chat ID")
        void updateChatId() {
            // given
            final var oldChatId = random.nextLong();
            final var newChatId = random.nextLong();
            billTestDataCreator.givenGroupExists(g -> g.setChatId(oldChatId));
            final var update = mockGroupUpdate(null, null, newChatId);
            final var message = update.message();
            when(message.migrateToChatId()).thenReturn(newChatId);
            when(message.migrateFromChatId()).thenReturn(oldChatId);
            // when
            listener.processUpdate(update, bot);
            // then
            assertTrue(groupRepository.findByChatId(newChatId).isPresent());
        }

        @Test
        @DisplayName("no chat ID migration")
        void noChatIdMigration() {
            // given
            final var chatId = random.nextLong();
            billTestDataCreator.givenGroupExists(g -> g.setChatId(chatId));
            final var update = mockGroupUpdate(null, null, chatId);
            final var message = update.message();
            when(message.migrateToChatId()).thenReturn(null);
            when(message.migrateFromChatId()).thenReturn(null);
            // when
            listener.processUpdate(update, bot);
            // then
            verify(groupRepository, never()).save(Mockito.any(Group.class));
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