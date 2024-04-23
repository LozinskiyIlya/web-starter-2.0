package com.starter.web.service;

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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;


class MessageProcessorIT extends AbstractSpringIntegrationTest {

    @Autowired
    private UserTestDataCreator userCreator;

    @Autowired
    private BillTestDataCreator billCreator;

    @Autowired
    private MessageProcessor messageProcessor;

    @SpyBean
    private OpenAiAssistant openAiAssistant;

    @Nested
    @DisplayName("Text messages")
    class TextMessages {

        @Test
        @DisplayName("Creates a bill if a message is payment-related")
        void createsBillIfMessageIsPaymentRelated() {
            final var message = "I owe you $100";
            doReturn(new MessageClassificationResponse(true))
                    .when(openAiAssistant).classifyMessage(message);
            doReturn(response("USD", 100.0))
                    .when(openAiAssistant).runTextPipeline(Mockito.eq(message), Mockito.any());
            // given
            var user = userCreator.givenUserExists();
            var group = billCreator.givenGroupExists(g -> g.setOwner(user));
            // when
            messageProcessor.processMessage(group, message);
            // then
            transactionTemplate.executeWithoutResult(tr -> {
                var bill = billCreator.billRepository().findAll().get(0);
                assertThat(bill.getGroup().getId()).isEqualTo(group.getId());
                assertThat(bill.getAmount()).isEqualTo(100);
                assertThat(bill.getCurrency()).isEqualTo("USD");
            });

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