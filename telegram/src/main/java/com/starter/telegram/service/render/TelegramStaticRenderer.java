package com.starter.telegram.service.render;

import com.pengrad.telegrambot.model.LinkPreviewOptions;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendAnimation;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.common.config.BetaFeaturesProperties;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


@Slf4j
public class TelegramStaticRenderer {

    public static final URI WEB_APP_DIRECT_URL = URI.create("https://t.me/ai_counting_bot/webapp");
    private static final String EXAMPLE_TEMPLATE = "Send bill information in any format.\nExample: #example#";
    private static final String BILL_SKIP_TEMPLATE = "Bill #id# skipped. <a href='#archive_url#'>Manage archive</a>";
    private static final String GROUP_TITLE_TEMPLATE = "\uD83D\uDC65 #num# groups:\n";
    private static final String GROUP_ENTRY_TEMPLATE = "◾\uFE0F <b>#title#</b>\n      #bills# bills • #members# members";
    private static final String DOCUMENTS_BETA = "<i>Document recognition is in beta and will soon be a premium feature. You can use image and PDF recognition now, but please double-check the results for accuracy.</i>";
    private static final String CHAT_WITH_BILLS_BETA = "<i>Chat feature is in beta and will soon be available to premium users only. Please double-check the results for accuracy.</i>";
    private static final String CURRENCY_SET_TEMPLATE = "Default currency <b>#code#</b>(#symbol#) set successfully for this chat.\nYou can always <a href='#change_link#'>change</a> it through the Web App interface.";
    private static final String CURRENCY_CHANGE_GIF_PATH = "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/CurrencyChange.mp4";


    public static String renderTags(Bill bill) {
        return bill.getTags().stream().map(tag -> "#" + tag.getName() + " ").reduce("", String::concat);
    }

    public static BaseRequest<?, ?> tryUpdateMessage(Long chatId, MaybeInaccessibleMessage message, String text, InlineKeyboardButton[]... buttons) {
        // if the message is accessible, update it
        final var keyboard = buttons != null && buttons.length > 0 ? new InlineKeyboardMarkup(buttons) : null;
        try {
            if (message instanceof Message && message.messageId() != Bill.DEFAULT_MESSAGE_ID) {
                final var editRequest = new EditMessageText(chatId, message.messageId(), text)
                        .parseMode(ParseMode.HTML)
                        .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true))
                        .disableWebPagePreview(true);
                if (keyboard != null) {
                    editRequest.replyMarkup(keyboard);
                }
                return editRequest;
            }
        } catch (Exception e) {
            log.error("Error while updating message", e);
        }
        // if the message is not accessible, send a new message
        return linkPreviewOff(new SendMessage(chatId, text), keyboard);
    }

    public static SendMessage linkPreviewOff(SendMessage request, InlineKeyboardMarkup keyboard) {
        if (keyboard != null) {
            request = request.replyMarkup(keyboard);
        }
        return request
                .parseMode(ParseMode.HTML)
                .linkPreviewOptions(new LinkPreviewOptions().isDisabled(true))
                .disableWebPagePreview(true);
    }

    public static String renderDate(Instant date) {
        final var zoneId = ZoneId.systemDefault();
        final var zonedDateTime = date.atZone(zoneId);
        final var formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");
        return zonedDateTime.format(formatter);
    }

    public static String renderId(UUID id) {
        return String.format("<code>#%s</code>", id.toString().substring(0, 8));
    }

    public static String renderTelegramUsername(UserInfo userInfo) {
        return userInfo.getTelegramUsername() != null ? userInfo.getTelegramUsername() : userInfo.getTelegramChatId().toString();
    }

    public static String renderWebAppDirectUrl(String paramName, UUID id) {
        return renderWebAppDirectUrl(paramName) + "_" + id;
    }

    public static String renderWebAppDirectUrl(String paramName) {
        return WEB_APP_DIRECT_URL + "?startapp=" + paramName;
    }

    public static SendMessage renderRecognizeMyBill(Long chatId) {
        final var textPart = EXAMPLE_TEMPLATE.replace("#example#", renderExample());
        return new SendMessage(chatId, textPart).parseMode(ParseMode.HTML);
    }

    public static BaseRequest<?, ?> renderBillSkipped(Long chatId, Bill bill, MaybeInaccessibleMessage message) {
        final var textPart = BILL_SKIP_TEMPLATE
                .replaceAll("#id#", renderId(bill.getId()))
                .replaceAll("#archive_url#", renderWebAppDirectUrl("archive", bill.getId()));
        return tryUpdateMessage(chatId, message, textPart);
    }

    public static BaseRequest<?, ?> renderAddMeRejectedUpdate(Long chatId, MaybeInaccessibleMessage message) {
        final var textPart = "Rejected";
        return tryUpdateMessage(chatId, message, textPart);
    }

    public static String renderAmount(Double amount, String currencySymbol) {
        final var symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        final var pattern = "#,##0.00";
        DecimalFormat decimalFormat = new DecimalFormat(pattern, symbols);
        decimalFormat.setGroupingSize(3);

        var formattedAmount = decimalFormat.format(amount);
        if (formattedAmount.endsWith(",00")) {
            formattedAmount = formattedAmount.substring(0, formattedAmount.length() - 3);
        } else if (formattedAmount.endsWith(",0")) {
            formattedAmount = formattedAmount.substring(0, formattedAmount.length() - 2);
        }
        return Matcher.quoteReplacement(formattedAmount + currencySymbol);
    }

    public static SendMessage renderPin(Long chatId) {
        return new SendMessage(chatId,
                """
                        Pin code is used to additionally protect your financial data. Store it in a safe place!
                        Please send me your new 6-digit pin code like this: /pin 123456
                        """
        );
    }

    public static String renderGroups(List<Pair<Group, Long>> groups) {
        return GROUP_TITLE_TEMPLATE.replace("#num#", String.valueOf(groups.size())) + "\n" +
                groups.stream()
                        .map(it -> GROUP_ENTRY_TEMPLATE
                                .replace("#title#", it.getFirst().getTitle())
                                .replace("#members#", "" + it.getFirst().getMembers().size())
                                .replace("#bills#", "" + it.getSecond()))
                        .collect(Collectors.joining("\n\n"));
    }

    public static String renderExample() {
        return "<code>" + randomExample() + "</code>";
    }

    public static String randomExample() {
        return NEW_BILL_EXAMPLES[(int) (Math.random() * NEW_BILL_EXAMPLES.length)];
    }

    public static String renderDocumentsBeta(BetaFeaturesProperties betaFeaturesProperties) {
        return betaFeaturesProperties.isDocumentsRecognition() ? DOCUMENTS_BETA : "";
    }

    public static String renderChatWithBillsBeta(BetaFeaturesProperties betaFeaturesProperties) {
        return betaFeaturesProperties.isChatWithBills() ? CHAT_WITH_BILLS_BETA : "";
    }

    public static SendAnimation renderCurrencySetMessage(Long chatId, String code, String symbol, UUID groupId) {
        final var textPart = CURRENCY_SET_TEMPLATE
                .replace("#code#", code)
                .replace("#symbol#", symbol)
                .replace("#change_link#", renderWebAppDirectUrl("group", groupId));
        return new SendAnimation(chatId, CURRENCY_CHANGE_GIF_PATH)
                .caption(textPart)
                .parseMode(ParseMode.HTML);
    }


    private static final String[] NEW_BILL_EXAMPLES = {
            "45$ dinner at Portabello Bistro",
            "120.50$ for groceries at Whole Foods",
            "15£ for taxi fare to downtown",
            "13.99$ for monthly subscription for Netflix",
            "4.50€ coffee at Starbucks",
            "60$ for gas station refill",
            "12.75$ for lunch at Chipotle",
            "35£ for office supplies from Staples",
            "25$ for movie night at AMC Theatres",
            "70$ for dinner at Olive Garden",
            "30$ gym membership this month",
            "150€ stay at Hilton",
            "350$ flight booking to New York",
            "85$ concert tickets for Coldplay",
            "45.20$ for books from Amazon",
            "19.99$ for online course from Udemy",
            "75$ for electricity bill",
            "40$ for water bill",
            "60$ for internet bill from Comcast",
            "55$ for phone bill from Verizon"
    };

}
