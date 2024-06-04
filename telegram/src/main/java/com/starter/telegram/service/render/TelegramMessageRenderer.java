package com.starter.telegram.service.render;


import com.pengrad.telegrambot.model.WebAppInfo;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.starter.common.config.ServerProperties;
import com.starter.common.service.CurrenciesService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.starter.telegram.listener.CallbackQueryUpdateListener.*;
import static com.starter.telegram.service.render.TelegramStaticRenderer.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageRenderer {
    private final static String ADD_ME_TEMPLATE = "add_me.txt";
    private final static String ADD_ME_APPROVED_TEMPLATE = "add_me_approved.txt";
    private final static String NEW_BILL_TEMPLATE = "new_bill.txt";
    private final static String BILL_TEMPLATE = "bill.txt";
    private final static String BILL_UPDATE_TEMPLATE = "#amount# confirmed. <a href='#edit_url#'>Edit</a>";
    private final static String BILL_SKIP_TEMPLATE = "Bill #id# skipped. <a href='#archive_url#'>Manage archive</a>";

    private final TemplateReader templateReader;

    private final ServerProperties serverProperties;

    private final CurrenciesService currenciesService;

    public SendMessage renderBill(Long chatId, Bill bill) {
        final var caption = renderCaption(bill);
        final var keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("\uD83D\uDDD1 Skip").callbackData(SKIP_BILL_PREFIX + bill.getId()),
                new InlineKeyboardButton("✏\uFE0F Edit").webApp(renderWebApp("bill", bill.getId().toString())),
                new InlineKeyboardButton("✅ Confirm").callbackData(CONFIRM_BILL_PREFIX + bill.getId())
        );
        return new SendMessage(chatId, caption).replyMarkup(keyboard).parseMode(ParseMode.HTML);
    }

    public SendMessage renderBillPreview(Long chatId, Bill bill) {
        final var caption = renderCaption(bill);
        return new SendMessage(chatId, caption).parseMode(ParseMode.HTML);
    }

    public BaseRequest<?, ?> renderBillUpdate(Long chatId, Bill bill, MaybeInaccessibleMessage message) {
        final var textPart = BILL_UPDATE_TEMPLATE
                .replaceAll("#amount#", renderAmount(bill.getAmount(), currenciesService.getSymbol(bill.getCurrency())))
                .replaceAll("#edit_url#", renderWebAppDirectUrl("bill", bill.getId()));
        return tryUpdateMessage(chatId, message, textPart);
    }

    public BaseRequest<?, ?> renderBillSkipped(Long chatId, Bill bill, MaybeInaccessibleMessage message) {
        final var textPart = BILL_SKIP_TEMPLATE
                .replaceAll("#id#", renderId(bill.getId()))
                .replaceAll("#archive_url#", renderWebAppDirectUrl("archive", bill.getId()));
        return tryUpdateMessage(chatId, message, textPart);
    }

    public SendMessage renderAddMeMessage(UserInfo owner, UserInfo requestingPermission, Group group) {
        log.info("Rendering add me message for user {} in group {}", requestingPermission, group);
        // message notifying an owner that a user wants to join the group with 2 buttons: accept and decline
        final var textPart = templateReader.read(ADD_ME_TEMPLATE)
                .replaceAll("#group_name#", group.getTitle())
                .replaceAll("#owner_name#", renderTelegramUsername(owner))
                .replaceAll("#user_name#", renderTelegramUsername(requestingPermission));
        final var keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌ Decline").callbackData(ADDME_REJECT_PREFIX),
                new InlineKeyboardButton("✅ Accept").callbackData(ADDME_ACCEPT_PREFIX + requestingPermission.getTelegramChatId()
                        + ID_SEPARATOR + group.getChatId())
        );
        return linkPreviewOff(new SendMessage(owner.getTelegramChatId(), textPart).replyMarkup(keyboard));
    }

    public BaseRequest<?, ?> renderAddMeAcceptedUpdate(Long chatId, MaybeInaccessibleMessage message, UserInfo userInfo, Group group) {
        final var textPart = templateReader.read(ADD_ME_APPROVED_TEMPLATE)
                .replaceAll("#edit_url#", renderWebAppDirectUrl("group", group.getId()))
                .replaceAll("#user_name#", renderTelegramUsername(userInfo))
                .replaceAll("#group_name#", group.getTitle());
        return tryUpdateMessage(chatId, message, textPart);
    }

    public BaseRequest<?, ?> renderAddMeRejectedUpdate(Long chatId, MaybeInaccessibleMessage message) {
        final var textPart = "Rejected";
        return tryUpdateMessage(chatId, message, textPart);
    }

    public SendMessage renderSettings(Long chatId) {
        return new SendMessage(chatId, "Settings").replyMarkup(new InlineKeyboardMarkup(
                new InlineKeyboardButton("View and edit").webApp(renderWebApp("settings", ""))
        ));
    }

    public SendMessage renderNewBill(Long chatId) {
        final var textPart = templateReader.read(NEW_BILL_TEMPLATE);
        return new SendMessage(chatId, textPart)
                .replyMarkup(new InlineKeyboardMarkup(
                        new InlineKeyboardButton("Add bill")
                                .webApp(renderWebApp("bill", "new"))))
                .parseMode(ParseMode.HTML);
    }

    public SendMessage renderPin(Long chatId) {
        return new SendMessage(chatId,
                """
                        Pin code is used to additionally protect your financial data. Store it in a safe place!
                        Please send me your new 6-digit pin code like this: /pin 123456
                        """
        );
    }

    private WebAppInfo renderWebApp(String path, String pathVariable) {
        return new WebAppInfo(serverProperties.getFrontendHost().resolve(path) + "/" + pathVariable);
    }

    private String renderCaption(Bill bill) {
        return templateReader.read(BILL_TEMPLATE)
                .replaceAll("#group_name#", bill.getGroup().getTitle())
                .replaceAll("#id#", renderId(bill.getId()))
                .replaceAll("#buyer#", bill.getBuyer())
                .replaceAll("#seller#", bill.getSeller())
                .replaceAll("#amount#", renderAmount(bill.getAmount(), currenciesService.getSymbol(bill.getCurrency())))
                .replaceAll("#purpose#", bill.getPurpose())
                .replaceAll("#date#", renderDate(bill.getMentionedDate()))
                .replaceAll("#tags#", renderTags(bill));
    }
}
