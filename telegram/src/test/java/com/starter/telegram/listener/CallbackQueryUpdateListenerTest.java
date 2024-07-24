package com.starter.telegram.listener;

import com.starter.domain.entity.Bill;
import com.starter.telegram.AbstractTelegramTest;
import com.starter.telegram.listener.query.CallbackQueryUpdateListener;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.starter.telegram.listener.query.AddmeCallbackExecutor.ADDME_ACCEPT_PREFIX;
import static com.starter.telegram.listener.query.AddmeCallbackExecutor.ADDME_REJECT_PREFIX;
import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.*;
import static com.starter.telegram.listener.query.BillCallbackExecutor.CONFIRM_BILL_PREFIX;
import static org.junit.jupiter.api.Assertions.*;


class CallbackQueryUpdateListenerTest extends AbstractTelegramTest {

    @Autowired
    private CallbackQueryUpdateListener listener;


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
            final var query = ADDME_ACCEPT_PREFIX + newMember.getTelegramChatId() + QUERY_SEPARATOR + group.getChatId();
            final var update = mockCallbackQueryUpdate(query, owner.getTelegramChatId());

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
            final var query = ADDME_ACCEPT_PREFIX + newMember.getTelegramChatId() + QUERY_SEPARATOR + group.getChatId();
            final var update = mockCallbackQueryUpdate(query, owner.getTelegramChatId());

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

            // when
            listener.processUpdate(update, bot);

            // then
            final var groupWithUser = billTestDataCreator.groupRepository().findById(group.getId()).orElseThrow();
            assertFalse(groupWithUser.contains(newMember));
            assertMessageSentToChatId(bot, owner.getTelegramChatId());
        }
    }

    @Nested
    @DisplayName("on confirm bill callback")
    class OnConfirmBillCallback {

        @Test
        @DisplayName("should confirm bill")
        void shouldConfirmBill() {
            // given
            final var chatId = random.nextLong();
            final var bill = billTestDataCreator.givenBillExists();
            final var query = CONFIRM_BILL_PREFIX + bill.getId();
            final var update = mockCallbackQueryUpdate(query, chatId);

            // when
            listener.processUpdate(update, bot);

            // then
            final var confirmedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertEquals(Bill.BillStatus.CONFIRMED, confirmedBill.getStatus());
        }
    }
}