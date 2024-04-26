package com.starter.telegram.listener;

import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.starter.telegram.listener.CallbackQueryUpdateListener.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CallbackQueryUpdateListenerTest extends AbstractUpdateListenerTest {

    @Autowired
    private CallbackQueryUpdateListener listener;

    @Autowired
    private UserTestDataCreator userTestDataCreator;

    @Autowired
    private BillTestDataCreator billTestDataCreator;


    @Nested
    @DisplayName("on add me callback")
    class OnAddmeCallback {

        @Transactional
        @Test
        @DisplayName("should add new member to the group if accepted")
        void shouldAddUserToGroup() {
            // given
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> {
            });
            final var group = billTestDataCreator.givenGroupExists(g -> g.setOwner(owner.getUser()));
            final var newMember = userTestDataCreator.givenUserInfoExists(ui -> {
            });
            final var query = ADDME_ACCEPT_PREFIX + newMember.getTelegramChatId() + ID_SEPARATOR + group.getChatId();
            final var update = mockCallbackQueryUpdate(query, owner.getTelegramChatId());
            final var bot = mockBot();

            // when
            listener.processUpdate(update, bot);

            // then
            final var groupWithUser = billTestDataCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertTrue(groupWithUser.contains(newMember.getUser()));
            assertMessageSentToChatId(bot, owner.getTelegramChatId());
        }

        @Transactional
        @Test
        @DisplayName("should work if already member")
        void shouldWorkIfAlreadyMember() {
            // given
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> {
            });
            final var newMember = userTestDataCreator.givenUserInfoExists(ui -> {
            });
            final var group = billTestDataCreator.givenGroupExists(g -> {
                g.setOwner(owner.getUser());
                g.setMembers(List.of(owner.getUser(), newMember.getUser()));
            });
            final var query = ADDME_ACCEPT_PREFIX + newMember.getTelegramChatId() + ID_SEPARATOR + group.getChatId();
            final var update = mockCallbackQueryUpdate(query, owner.getTelegramChatId());
            final var bot = mockBot();

            // when
            listener.processUpdate(update, bot);

            // then
            final var groupWithUser = billTestDataCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertTrue(groupWithUser.contains(newMember.getUser()));
            assertMessageSentToChatId(bot, owner.getTelegramChatId());
        }

        @Transactional
        @Test
        @DisplayName("should reject addme request")
        void shouldRejectAddmeRequest() {
            // given
            final var owner = userTestDataCreator.givenUserInfoExists(ui -> {
            });
            final var group = billTestDataCreator.givenGroupExists(g -> g.setOwner(owner.getUser()));
            final var newMember = userTestDataCreator.givenUserInfoExists(ui -> {
            }).getUser();
            final var update = mockCallbackQueryUpdate(ADDME_REJECT_PREFIX, owner.getTelegramChatId());
            final var bot = mockBot();

            // when
            listener.processUpdate(update, bot);

            // then
            final var groupWithUser = billTestDataCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertFalse(groupWithUser.contains(newMember));
            assertMessageSentToChatId(bot, owner.getTelegramChatId());
        }
    }
}