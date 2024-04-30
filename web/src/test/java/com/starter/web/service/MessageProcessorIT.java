package com.starter.web.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.response.SendResponse;
import com.starter.common.events.TelegramTextMessageEvent;
import com.starter.domain.entity.Bill;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


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
                    .when(openAiAssistant).runTextPipeline(Mockito.any(), Mockito.eq(message));
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
            await().atMost(2, TimeUnit.SECONDS).until(() -> billRepository.findAllByGroup(group).size() == 1);
            var bill = billRepository.findAllByGroup(group).get(0);
            assertThat(bill.getGroup().getId()).isEqualTo(group.getId());
            assertThat(bill.getAmount()).isEqualTo(100);
            assertThat(bill.getCurrency()).isEqualTo("USD");
            assertThat(bill.getStatus()).isEqualTo(Bill.BillStatus.SENT);
            assertThat(bill.getMessageId()).isEqualTo(tgMessageId);
        }
    }

    private BillAssistantResponse response(String currency, Double amount) {
        final var response = BillAssistantResponse.EMPTY();
        response.setCurrency(currency);
        response.setAmount(amount);
        response.setTags(new String[]{"Work"});
        return response;
    }
}