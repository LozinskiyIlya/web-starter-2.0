package com.starter.telegram.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineQueryResultArticle;
import com.pengrad.telegrambot.request.AnswerInlineQuery;
import com.starter.domain.entity.Bill;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.domain.repository.UserInfoRepository;
import com.starter.telegram.service.render.TelegramMessageRenderer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class InlineQueryUpdateListener implements UpdateListener {
    public static final int INLINE_RESULT_PAGE_SIZE = 5;

    private final TelegramMessageRenderer renderer;
    private final UserInfoRepository userInfoRepository;
    private final BillRepository billRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public void processUpdate(Update update, TelegramBot bot) {
        final var query = update.inlineQuery().query();
        if (query.length() < 3) {
            return;
        }
        final var chatId = update.inlineQuery().from().id();
        final var user = userInfoRepository.findByTelegramChatId(chatId).orElseThrow().getUser();
        final var groups = groupRepository.findAllByOwner(user);
        final var offsetString = update.inlineQuery().offset();
        final var offset = offsetString.isEmpty() ? 0 : Integer.parseInt(offsetString);
        final var pageRequest = createPageRequest(offset);
        final var bills = billRepository.findNotSkippedByGroupAndTagAndPurpose(groups, query.trim(), pageRequest);
        final var results = bills
                .stream()
                .map(renderer::renderBillInline)
                .toArray(InlineQueryResultArticle[]::new);
        final var answer = new AnswerInlineQuery(update.inlineQuery().id(), results)
                .nextOffset(calculateNextOffset(bills, offset))
                .button(renderer.inlineQueryButton())
                .cacheTime(0)
                .isPersonal(true);
        bot.execute(answer);
    }

    private PageRequest createPageRequest(int offset) {
        int pageNumber = offset / INLINE_RESULT_PAGE_SIZE;
        return PageRequest.of(pageNumber, INLINE_RESULT_PAGE_SIZE);
    }

    private String calculateNextOffset(Page<Bill> page, int offset) {
        return page.hasNext() ? String.valueOf(offset + INLINE_RESULT_PAGE_SIZE) : "";
    }
}
