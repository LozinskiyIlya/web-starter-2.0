package com.starter.web.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.SendResponse;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.testdata.BillTestDataCreator;
import com.starter.domain.repository.testdata.UserTestDataCreator;
import com.starter.web.AbstractSpringIntegrationTest;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.fragments.MessageClassificationResponse;
import com.starter.web.service.openai.OpenAiAssistant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.starter.telegram.service.TelegramBillService.NOT_RECOGNIZED_MESSAGE;
import static com.starter.telegram.service.TelegramBillService.PROCESSING_ERROR_MESSAGE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class MessageProcessorIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private BillTestDataCreator billCreator;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private MessageProcessor messageProcessor;

    @SpyBean
    private OpenAiAssistant openAiAssistant;

    @SpyBean
    private TelegramBot bot;

    @Nested
    @DisplayName("Text messages")
    class TextMessages {

        @Test
        @DisplayName("Creates a bill if a message is payment-related")
        void createsBillIfMessageIsPaymentRelated() {
            final var message = "I owe you $100";
            final var tgMessageId = 1;
            doReturn(new MessageClassificationResponse(true))
                    .when(openAiAssistant).classifyMessage(message);
            doReturn(response("USD", 100.0))
                    .when(openAiAssistant).runTextPipeline(Mockito.any(), Mockito.eq(message), Mockito.any());
            final var responseMocked = Mockito.mock(SendResponse.class);
            final var messageMocked = Mockito.mock(com.pengrad.telegrambot.model.Message.class);
            when(messageMocked.messageId()).thenReturn(tgMessageId);
            when(responseMocked.message()).thenReturn(messageMocked);
            doReturn(responseMocked).when(bot).execute(Mockito.any());
            // given
            var user = userCreator.givenUserInfoExists(ui -> {
            }).getUser();
            var group = billCreator.givenGroupExists(g -> g.setOwner(user));
            // when
            messageProcessor.processMessage(new TelegramTextMessageEvent(this, Pair.of(group.getId(), message)));
            // then - bill is created asynchronously
            await().pollDelay(2, TimeUnit.SECONDS).until(() -> billRepository.findAllByGroup(group).size() == 1);
            var bill = billRepository.findAllByGroup(group).get(0);
            assertThat(bill.getGroup().getId()).isEqualTo(group.getId());
            assertThat(bill.getAmount()).isEqualTo(100);
            assertThat(bill.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Sends a message if a message is not payment-related")
        void sendsMessageIfMessageIsNotPaymentRelated() {
            final var message = "Hello";
            doReturn(new MessageClassificationResponse(false))
                    .when(openAiAssistant).classifyMessage(message);
            doReturn(BillAssistantResponse.EMPTY())
                    .when(openAiAssistant).runTextPipeline(Mockito.any(), Mockito.eq(message), Mockito.any());
            // given
            var user = userCreator.givenUserInfoExists(ui -> {
            }).getUser();
            var group = billCreator.givenGroupExists(g -> g.setOwner(user));
            // when
            messageProcessor.processMessage(new TelegramTextMessageEvent(this, Pair.of(group.getId(), message)));
            // then
            await().pollDelay(5, TimeUnit.SECONDS).until(() -> true);
            assertSentMessageToChatIdContainsText(bot, group.getChatId(), NOT_RECOGNIZED_MESSAGE);
        }

        @Test
        @DisplayName("Rejects zero amount bills by default")
        void rejectsZeroAmountBillsByDefault() {
            final var message = "Breakfast - $0";
            doReturn(new MessageClassificationResponse(true))
                    .when(openAiAssistant).classifyMessage(message);
            doReturn(response("USD", 0.001))
                    .when(openAiAssistant).runTextPipeline(Mockito.any(), Mockito.eq(message), Mockito.any());
            // given
            var user = userCreator.givenUserInfoExists(ui -> {
            }).getUser();
            userCreator.givenUserSettingsExists(us -> us.setUser(user));
            var group = billCreator.givenGroupExists(g -> g.setOwner(user));
            // when
            messageProcessor.processMessage(new TelegramTextMessageEvent(this, Pair.of(group.getId(), message)));
            // then
            await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);
            assertSentMessageToChatIdContainsText(bot, group.getChatId(), NOT_RECOGNIZED_MESSAGE);
        }

        @Test
        @DisplayName("Accepts zero amount bills according to settings")
        void acceptsZeroAmountBills() {
            final var message = "Breakfast - $0";
            doReturn(new MessageClassificationResponse(true))
                    .when(openAiAssistant).classifyMessage(message);
            doReturn(response("USD", 0d))
                    .when(openAiAssistant).runTextPipeline(Mockito.any(), Mockito.eq(message), Mockito.any());
            // given
            var user = userCreator.givenUserInfoExists(ui -> {
            }).getUser();
            userCreator.givenUserSettingsExists(us -> {
                us.setUser(user);
                us.setSkipZeros(false);
            });
            var group = billCreator.givenGroupExists(g -> g.setOwner(user));
            // when
            messageProcessor.processMessage(new TelegramTextMessageEvent(this, Pair.of(group.getId(), message)));
            // then
            await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);
            var bill = billRepository.findAllByGroup(group).get(0);
            assertThat(bill.getGroup().getId()).isEqualTo(group.getId());
            assertThat(bill.getAmount()).isEqualTo(0);
            assertThat(bill.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Notifies if error occurred")
        void notifiesIfErrorOccurred() {
            final var message = "I owe you $100";
            doReturn(new MessageClassificationResponse(true))
                    .when(openAiAssistant).classifyMessage(message);
            doThrow(new RuntimeException("Timeout"))
                    .when(openAiAssistant).runTextPipeline(Mockito.any(), Mockito.eq(message), Mockito.any());
            // given
            var user = userCreator.givenUserInfoExists(ui -> {
            }).getUser();
            var group = billCreator.givenGroupExists(g -> g.setOwner(user));
            // when
            messageProcessor.processMessage(new TelegramTextMessageEvent(this, Pair.of(group.getId(), message)));
            // then
            await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);
            assertSentMessageToChatIdContainsText(bot, group.getChatId(), PROCESSING_ERROR_MESSAGE);
        }
    }

    private BillAssistantResponse response(String currency, Double amount) {
        final var response = BillAssistantResponse.EMPTY();
        response.setCurrency(currency);
        response.setAmount(amount);
        response.setTags(new String[]{"Work"});
        return response;
    }

    protected static void assertSentMessageToChatIdContainsText(TelegramBot bot, Long chatId, String shouldContain) {
        final var foundTexts = new LinkedList<>();
        final var containsTimes = getCapturedRequestParams(bot)
                .filter(params -> params.get("chat_id").equals(chatId))
                .map(params -> params.get("text"))
                .peek(foundTexts::add)
                .filter(text -> ((String) text).contains(shouldContain))
                .count();
        assertEquals(1, containsTimes, "Message containing: \"" + shouldContain + "\" was not found. Present texts: " + foundTexts);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<Map> getCapturedRequestParams(TelegramBot bot) {
        final var captor = ArgumentCaptor.forClass(BaseRequest.class);
        verify(bot, atLeast(0)).execute(captor.capture());
        return captor.getAllValues()
                .stream()
                .map(BaseRequest::getParameters);
    }
}