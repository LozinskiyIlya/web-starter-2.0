package com.starter.telegram.service.render;


import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.WebAppInfo;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.starter.telegram.listener.CallbackQueryUpdateListener.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageRenderer {
    private final static String ADD_ME_TEMPLATE = "add_me.txt";
    private final static String BILL_TEMPLATE = "bill.txt";
    private final static String BILL_UPDATE_TEMPLATE = "bill_update.txt";

    private final TemplateReader templateReader;

    public String renderMessage(String message) {
        return message;
    }

    public SendMessage renderBill(Long chatId, Bill bill) {
        final var textPart = templateReader.read(BILL_TEMPLATE)
                .replaceAll("#group_name#", bill.getGroup().getTitle())
                .replaceAll("#id#", renderId(bill.getId()))
                .replaceAll("#buyer#", bill.getBuyer())
                .replaceAll("#seller#", bill.getSeller())
                .replaceAll("#amount#", bill.getAmount() + " " + bill.getCurrency())
                .replaceAll("#purpose#", bill.getPurpose())
                .replaceAll("#date#", renderDate(bill.getMentionedDate()))
                .replaceAll("#tags#", bill.getTags().stream().map(tag -> "#" + tag.getName()).reduce("", String::concat));
        final var keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("✏\uFE0F Edit").webApp(new WebAppInfo("https://example.com")),
                new InlineKeyboardButton("✅ Confirm").callbackData(CONFIRM_BILL_PREFIX + bill.getId())
        );

        return new SendMessage(chatId, textPart).replyMarkup(keyboard).parseMode(ParseMode.HTML);
    }

    private String renderDate(Instant date) {
        // Define the time zone
        ZoneId zoneId = ZoneId.systemDefault(); // Or specify a ZoneId like ZoneId.of("Europe/Paris")

        // Convert Instant to ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(zoneId);

        // Define the date time format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

        // Format the ZonedDateTime
        return zonedDateTime.format(formatter);
    }

    private String renderId(UUID id) {
        return String.format("<code>#%s</code>", id.toString().substring(0, 8));
    }

    public BaseRequest<?, ?> renderBillUpdate(Long chatId, Bill bill, MaybeInaccessibleMessage message) {
        final var textPart = templateReader.read(BILL_UPDATE_TEMPLATE)
                .replaceAll("#id#", renderId(bill.getId()))
                .replaceAll("#edit_url#", "https://t.me/ai_counting_bot/webapp");
        if (message instanceof Message) {
            // Update the original message instead of sending a new one
            return new EditMessageText(chatId, message.messageId(), textPart)
                    .parseMode(ParseMode.HTML)
                    .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true))
                    .disableWebPagePreview(true);
        }
        // Original message is not accessible, send a new one
        return new SendMessage(chatId, textPart)
                .parseMode(ParseMode.HTML)
                .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true))
                .disableWebPagePreview(true);
    }

    public SendMessage renderAddMeMessage(UserInfo owner, UserInfo requestingPermission, Group group) {
        log.info("Rendering add me message for user {} in group {}", requestingPermission, group);
        // message notifying an owner that a user wants to join the group with 2 buttons: accept and decline
        final var textPart = templateReader.read(ADD_ME_TEMPLATE)
                .replaceAll("#group_name#", group.getTitle())
                .replaceAll("#owner_name#", owner.getFirstName())
                .replaceAll("#user_name#", requestingPermission.getTelegramUsername());
        final var keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("✅ Accept").callbackData(ADDME_ACCEPT_PREFIX + requestingPermission.getUser().getId()),
                new InlineKeyboardButton("❌ Decline").callbackData(ADDME_REJECT_PREFIX + requestingPermission.getUser().getId())
        );
        return new SendMessage(owner.getTelegramChatId(), textPart).replyMarkup(keyboard).parseMode(ParseMode.HTML);
    }
}
