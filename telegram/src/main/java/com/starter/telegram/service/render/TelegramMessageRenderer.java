package com.starter.telegram.service.render;


import com.pengrad.telegrambot.model.WebAppInfo;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.domain.entity.Bill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.starter.telegram.service.listener.CallbackQueryUpdateListener.CONFIRM_BILL_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageRenderer {

    private final static String BILL_TEMPLATE = "bill.txt";
    private final TemplateReader templateReader;

    public String renderMessage(String message) {
        return message;
    }

    public SendMessage renderBill(Long chatId, Bill bill) {
        final var textPart = templateReader.read(BILL_TEMPLATE)
                .replaceAll("#group_name#", bill.getGroup().getTitle())
                .replaceAll("#id#", bill.getId().toString().substring(0, 8))
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
}
