package com.starter.telegram.service.render;


import com.pengrad.telegrambot.model.WebAppInfo;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.common.config.ServerProperties;
import com.starter.common.service.CurrenciesService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.entity.UserSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static com.starter.telegram.listener.CallbackQueryUpdateListener.*;
import static com.starter.telegram.service.TelegramBotService.latestKeyboard;
import static com.starter.telegram.service.render.TelegramStaticRenderer.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageRenderer {
    private final static String START_COMMAND_TEMPLATE = "start.txt";
    private final static String ADD_ME_TEMPLATE = "add_me.txt";
    private final static String ADD_ME_APPROVED_TEMPLATE = "add_me_approved.txt";
    private final static String NEW_BILL_TEMPLATE = "new_bill.txt";
    private final static String BILL_TEMPLATE = "bill.txt";
    private final static String DAILY_REMINDER_TEMPLATE = "daily.txt";
    private final static String BILL_CONFIRMED_TEMPLATE = "#amount# confirmed. <a href='#edit_url#'>Edit</a>";
    private final static String BILL_SKIP_TEMPLATE = "Bill #id# skipped. <a href='#archive_url#'>Manage archive</a>";
    private final static String EXAMPLE_TEMPLATE = "Send bill information in any format.\nExample: <i>#example#</i>";

    private final TemplateReader templateReader;

    private final ServerProperties serverProperties;

    private final CurrenciesService currenciesService;

    public SendMessage renderBill(Long chatId, Bill bill, boolean spoiler) {
        final var caption = renderCaption(bill, spoiler);
        final var keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("\uD83D\uDDD1 Skip").callbackData(SKIP_BILL_PREFIX + bill.getId()),
                new InlineKeyboardButton("✏\uFE0F Edit").webApp(renderWebApp("bill", bill.getId().toString())),
                new InlineKeyboardButton("✅ Confirm").callbackData(CONFIRM_BILL_PREFIX + bill.getId())
        );
        return new SendMessage(chatId, caption).replyMarkup(keyboard).parseMode(ParseMode.HTML);
    }

    public SendMessage renderBillPreview(Long chatId, Bill bill, boolean spoiler) {
        final var caption = renderCaption(bill, spoiler);
        return new SendMessage(chatId, caption).parseMode(ParseMode.HTML);
    }

    public BaseRequest<?, ?> renderBillConfirmation(Long chatId, Bill bill, MaybeInaccessibleMessage message) {
        final var textPart = BILL_CONFIRMED_TEMPLATE
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

    public SendMessage renderDailyReminder(UserSettings settings) {
        final var userInfo = settings.getUser().getUserInfo();
        final var textPart = templateReader.read(DAILY_REMINDER_TEMPLATE)
                .replace("#name#", userInfo.getFirstName())
                .replace("#settings_url#", renderWebAppDirectUrl("settings"));
        return new SendMessage(userInfo.getTelegramChatId(), textPart)
                .disableNotification(settings.getSilentMode())
                .disableWebPagePreview(true)
                .parseMode(ParseMode.HTML)
                .replyMarkup(latestKeyboard());
    }

    public SendMessage renderNewBill(Long chatId) {
        final var textPart = templateReader.read(NEW_BILL_TEMPLATE)
                .replace("#example#", randomExample());
        return new SendMessage(chatId, textPart)
                .replyMarkup(
                        new InlineKeyboardMarkup(
                                new InlineKeyboardButton[]{new InlineKeyboardButton("⌨\uFE0F OK, recognize my bill")
                                        .callbackData(RECOGNIZE_BILL_PREFIX)},
                                new InlineKeyboardButton[]{new InlineKeyboardButton("\uD83E\uDDFE I'll use the form")
                                        .webApp(renderWebApp("bill", "new"))}
                        ))
                .parseMode(ParseMode.HTML);
    }

    public SendMessage renderRecognizeMyBill(Long chatId) {
        final var textPart = EXAMPLE_TEMPLATE.replace("#example#", randomExample());
        return new SendMessage(chatId, textPart).parseMode(ParseMode.HTML);
    }

    public SendMessage renderStartMessage(Long chatId, String firstName) {
        final var textPart = templateReader.read(START_COMMAND_TEMPLATE)
                .replace("#name#", StringUtils.hasText(firstName) ? firstName : "Anonymous")
                .replace("#example#", randomExample());
        return new SendMessage(chatId, textPart).replyMarkup(latestKeyboard()).parseMode(ParseMode.HTML);
    }

    private WebAppInfo renderWebApp(String path, String pathVariable) {
        return new WebAppInfo(serverProperties.getFrontendHost().resolve(path) + "/" + pathVariable);
    }

    private String renderCaption(Bill bill, boolean spoiler) {
        final var caption = templateReader.read(BILL_TEMPLATE)
                .replaceAll("#group_name#", bill.getGroup().getTitle())
                .replaceAll("#id#", renderId(bill.getId()))
                .replaceAll("#buyer#", bill.getBuyer())
                .replaceAll("#seller#", bill.getSeller())
                .replaceAll("#amount#", renderAmount(bill.getAmount(), currenciesService.getSymbol(bill.getCurrency())))
                .replaceAll("#purpose#", bill.getPurpose())
                .replaceAll("#date#", renderDate(bill.getMentionedDate()))
                .replaceAll("#tags#", renderTags(bill));
        if (!spoiler) {
            return caption
                    .replaceAll("<tg-spoiler>", "")
                    .replaceAll("</tg-spoiler>", "");
        }
        return caption;
    }
}
