package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.starter.common.service.CurrenciesService;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.GroupRepository;
import com.starter.telegram.service.TelegramUserService;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.starter.telegram.service.render.TelegramStaticRenderer.renderCurrencyExpectedMessage;
import static com.starter.telegram.service.render.TelegramStaticRenderer.renderCurrencySetMessage;

@Slf4j
@Component
public class PrivateChatTextUpdateListener extends AbstractChatUpdateListener {

    private final CurrenciesService currenciesService;

    public PrivateChatTextUpdateListener(
            TelegramUserService telegramUserService,
            TelegramMessageRenderer telegramMessageRenderer,
            GroupRepository groupRepository,
            ApplicationEventPublisher publisher,
            @Qualifier("downloadDirectory") String downloadDirectory, CurrenciesService currenciesService) {
        super(telegramUserService, telegramMessageRenderer, groupRepository, publisher, downloadDirectory);
        this.currenciesService = currenciesService;
    }

    @Override
    protected Group getGroup(Update update, TelegramBot bot) {
        // here we are in private messages, find a personal "group" with same chatId
        // as user's chatId or create new "group" if not found
        final var chatId = update.message().from().id();
        return groupRepository.findByChatId(chatId).orElseGet(() -> {
            final var personal = Group.personal(chatId);
            final var owner = telegramUserService.createOrFindUser(update.message().from(), bot);
            personal.setOwner(owner);
            personal.setMembers(List.of(owner));
            return groupRepository.save(personal);
        });
    }

    @Override
    protected void checkIdMigration(Update update) {
        // never happens in private messages - do nothing
    }

    @Override
    public void processUpdate(Update update, TelegramBot bot) {
        final var personal = getGroup(update, bot);
        if (StringUtils.hasText(personal.getDefaultCurrency())) {
            super.processUpdate(update, bot);
        } else {
            final var code = update.message().text().toUpperCase();
            final var chatId = update.message().from().id();
            if (currenciesService.containsCode(code)) {
                personal.setDefaultCurrency(code);
                groupRepository.save(personal);
                final var message = renderCurrencySetMessage(chatId, code, currenciesService.getSymbol(code), personal.getId());
                bot.execute(message);
            } else {
                final var message = renderCurrencyExpectedMessage(chatId);
                bot.execute(message);
            }
        }
    }
}
