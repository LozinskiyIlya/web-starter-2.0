package com.starter.telegram.service;

import com.pengrad.telegrambot.request.EditMessageText;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.UserInfo;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TelegramBillServiceTest extends AbstractTelegramTest {

    @Autowired
    private TelegramBillService service;

    @Nested
    @DisplayName("on bill created")
    class OnBillCreated {
    }

    @Nested
    @DisplayName("on bill confirmed")
    class OnBillConfirmed {

        @Test
        @DisplayName("should change status")
        void shouldChangeStatus() {
            // given
            final var bill = billTestDataCreator.givenBillExists(b -> b.setStatus(Bill.BillStatus.SENT));

            // when
            service.onBillConfirmed(new BillConfirmedEvent(this, bill.getId()));

            // then
            final var updatedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertEquals(Bill.BillStatus.CONFIRMED, updatedBill.getStatus());
        }

        @Test
        @DisplayName("should update bill message")
        void shouldUpdateMessage() {
            // given
            final var ownerInfo = userTestDataCreator.givenUserInfoExists();
            final var bill = billTestDataCreator.givenBillExists(b -> {
                b.setGroup(billTestDataCreator.givenGroupExists(g -> g.setOwner(ownerInfo.getUser())));
                b.setAmount(100d);
                b.setCurrency("USD");
                b.setMessageId(random.nextInt());
            });
            // when
            service.onBillConfirmed(new BillConfirmedEvent(this, bill.getId()));
            //then
            verify(bot).execute(Mockito.any(EditMessageText.class));
            assertMessageSentToChatId(bot, ownerInfo.getTelegramChatId());
            assertSentMessageContainsText(bot, "100.0$ confirmed.");
        }

        @Test
        @DisplayName("should send preview to members")
        void shouldSendPreviewToMembers() {
            // given
            final var ownerInfo = userTestDataCreator.givenUserInfoExists();
            final var memberInfo = userTestDataCreator.givenUserInfoExists();
            final var otherMemberInfo = userTestDataCreator.givenUserInfoExists();
            final var members = List.of(ownerInfo, memberInfo, otherMemberInfo);
            final var bill = billTestDataCreator.givenBillExists(b ->
                    b.setGroup(billTestDataCreator.givenGroupExists(g -> {
                        g.setOwner(ownerInfo.getUser());
                        g.setMembers(members.stream().map(UserInfo::getUser).toList());
                    })));
            // when
            service.onBillConfirmed(new BillConfirmedEvent(this, bill.getId()));
            //then
            verify(bot, never()).execute(Mockito.any(EditMessageText.class));
            assertMessageNotSentToChatId(bot, ownerInfo.getTelegramChatId());
            assertSentMessageToChatIdContainsText(bot, bill.getPurpose(), memberInfo.getTelegramChatId());
            assertSentMessageToChatIdContainsText(bot, bill.getPurpose(), otherMemberInfo.getTelegramChatId());
        }

        @Test
        @DisplayName("should not send preview to members if already confirmed")
        void shouldNotSendPreviewToMembersIfConfirmedPriorly() {
            // given
            final var ownerInfo = userTestDataCreator.givenUserInfoExists();
            final var memberInfo = userTestDataCreator.givenUserInfoExists();
            final var otherMemberInfo = userTestDataCreator.givenUserInfoExists();
            final var members = List.of(ownerInfo, memberInfo, otherMemberInfo);
            final var bill = billTestDataCreator.givenBillExists(b -> {
                b.setStatus(Bill.BillStatus.CONFIRMED);
                b.setGroup(billTestDataCreator.givenGroupExists(g -> {
                    g.setOwner(ownerInfo.getUser());
                    g.setMembers(members.stream().map(UserInfo::getUser).toList());
                }));
            });
            // when
            service.onBillConfirmed(new BillConfirmedEvent(this, bill.getId()));
            //then
            verify(bot, never()).execute(Mockito.any(EditMessageText.class));
            assertMessageNotSentToChatId(bot, memberInfo.getTelegramChatId());
            assertMessageNotSentToChatId(bot, otherMemberInfo.getTelegramChatId());
        }
    }
}