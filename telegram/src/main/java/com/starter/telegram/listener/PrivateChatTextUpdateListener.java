package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.common.service.CurrenciesService;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.TelegramStateMachine;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.starter.telegram.service.TelegramStateMachine.State.SET_CURRENCY;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderCurrencyInvalidFormat;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderCurrencySetMessage;

@Slf4j
@Component
public class PrivateChatTextUpdateListener extends AbstractChatUpdateListener {

    private final TelegramStateMachine stateMachine;
    private final CurrenciesService currenciesService;

    public PrivateChatTextUpdateListener(
            TelegramUserService telegramUserService,
            TelegramMessageRenderer telegramMessageRenderer,
            GroupRepository groupRepository,
            ApplicationEventPublisher publisher,
            @Qualifier("downloadDirectory") String downloadDirectory,
            TelegramStateMachine telegramStateMachine,
            CurrenciesService currenciesService) {
        super(telegramUserService, telegramMessageRenderer, groupRepository, publisher, downloadDirectory);
        this.stateMachine = telegramStateMachine;
        this.currenciesService = currenciesService;
    }

    @Override
    protected Group getGroup(Update update, TelegramBot bot) {
        return telegramUserService.createOrFindPersonalGroup(update.message().from(), bot);
    }

    @Override
    protected void checkIdMigration(Update update) {
        // never happens in private messages - do nothing
    }

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var chatId = update.message().from().id();
        if (stateMachine.inState(chatId, SET_CURRENCY)) {
            final var personal = getGroup(update, bot);
            final var code = update.message().text().toUpperCase();
            if (currenciesService.containsCode(code)) {
                personal.setDefaultCurrency(code);
                groupRepository.save(personal);
                final var message = renderCurrencySetMessage(chatId, code, currenciesService.getSymbol(code), personal.getId());
                bot.execute(message);
                stateMachine.removeState(chatId);
            } else {
                final var message = renderCurrencyInvalidFormat(chatId);
                bot.execute(message);
            }
            return;
        }
        super.processUpdate(update, bot);
    }
}
