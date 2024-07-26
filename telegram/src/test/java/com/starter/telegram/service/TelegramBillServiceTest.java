package com.starter.telegram.service;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.starter.common.events.BillConfirmedEvent;
import com.starter.common.events.BillCreatedEvent;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.UserInfo;
import com.starter.telegram.AbstractTelegramTest;
import org.junit.jupiter.api.BeforeEach;
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

        @BeforeEach
        void setupBotResponse() {
            final var responseMock = Mockito.mock(SendResponse.class);
            final var messageMock = Mockito.mock(Message.class);
            Mockito.when(responseMock.message()).thenReturn(messageMock);
            Mockito.when(messageMock.messageId()).thenReturn(random.nextInt());
            Mockito.when(bot.execute(Mockito.any(SendMessage.class)))
                    .thenReturn(responseMock);
        }

        @Test
        @DisplayName("should send bill to confirmation")
        void shouldSendBillToConfirmation() {
            // given
            final var ownerInfo = userTestDataCreator.givenUserInfoExists();
            final var bill = billTestDataCreator.givenBillExists(b ->
                    b.setGroup(billTestDataCreator.givenGroupExists(g ->
                            g.setOwner(ownerInfo.getUser()))));
            // when
            service.onBillCreated(new BillCreatedEvent(this, bill.getId()));
            // then
            verify(bot).execute(Mockito.any(SendMessage.class));
            assertSentMessageToChatIdContainsText(bot, ownerInfo.getTelegramChatId(), "<tg-spoiler>");
            // and then bill status changed to SENT
            final var updatedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertEquals(Bill.BillStatus.SENT, updatedBill.getStatus());
        }

        @Test
        @DisplayName("should not send bill to confirmation if auto-confirm enabled")
        void shouldNotSendBillToConfirmationIfAutoConfirm() {
            // given
            final var ownerInfo = userTestDataCreator.givenUserInfoExists();
            final var bill = billTestDataCreator.givenBillExists(b ->
                    b.setGroup(billTestDataCreator.givenGroupExists(g -> {
                        g.setOwner(ownerInfo.getUser());
                        g.setMembers(List.of(ownerInfo.getUser()));
                    })));
            userTestDataCreator.givenUserSettingsExists(s -> {
                s.setAutoConfirmBills(true);
                s.setUser(ownerInfo.getUser());
            });
            // when
            service.onBillCreated(new BillCreatedEvent(this, bill.getId()));
            // then does not send new bill message, but sends only confirmation
            assertSentMessageNotContainsText(bot, "New bill");
            assertSentMessageContainsText(bot, "confirmed.");
            // and then bill status changed to CONFIRMED
            final var updatedBill = billTestDataCreator.billRepository().findById(bill.getId()).orElseThrow();
            assertEquals(Bill.BillStatus.CONFIRMED, updatedBill.getStatus());
        }

        @Test
        @DisplayName("should remove <tg-spoiler> according to user settings")
        void shouldRemoveSpoilerAccordingToUserSettings() {
            // given
            final var ownerInfo = userTestDataCreator.givenUserInfoExists();
            final var bill = billTestDataCreator.givenBillExists(b ->
                    b.setGroup(billTestDataCreator.givenGroupExists(g ->
                            g.setOwner(ownerInfo.getUser()))));
            userTestDataCreator.givenUserSettingsExists(s -> {
                s.setSpoilerBills(false);
                s.setUser(ownerInfo.getUser());
            });
            // when
            service.onBillCreated(new BillCreatedEvent(this, bill.getId()));
            // then
            verify(bot).execute(Mockito.any(SendMessage.class));
            assertMessageSentToChatId(bot, ownerInfo.getTelegramChatId());
            assertSentMessageNotContainsText(bot, "<tg-spoiler>");
        }
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
                b.setAmount(100.0);
                b.setCurrency("USD");
                b.setMessageId(random.nextInt());
            });
            // when
            service.onBillConfirmed(new BillConfirmedEvent(this, bill.getId()));
            //then
            verify(bot).execute(Mockito.any(EditMessageText.class));
            assertMessageSentToChatId(bot, ownerInfo.getTelegramChatId());
            assertSentMessageContainsText(bot, "100$ confirmed.");
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

            //then bot sends a confirmation to owner
            assertSentMessageToChatIdContainsText(bot, ownerInfo.getTelegramChatId(), "confirmed. <a href='");

            //and then bot sends preview to other group members
            assertSentMessageToChatIdContainsText(bot, memberInfo.getTelegramChatId(), bill.getPurpose());
            assertSentMessageToChatIdContainsText(bot, otherMemberInfo.getTelegramChatId(), bill.getPurpose());
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