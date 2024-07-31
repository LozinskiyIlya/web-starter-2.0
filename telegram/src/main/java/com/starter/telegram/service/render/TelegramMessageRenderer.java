package com.starter.telegram.service.render;


import com.pengrad.telegrambot.model.WebAppInfo;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.starter.common.config.BetaFeaturesProperties;
import com.starter.common.config.ServerProperties;
import com.starter.common.service.CurrenciesService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.entity.UserSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.starter.domain.repository.BillRepository.TagAmount;
import static com.starter.telegram.listener.query.AddmeCallbackExecutor.ADDME_ACCEPT_PREFIX;
import static com.starter.telegram.listener.query.AddmeCallbackExecutor.ADDME_REJECT_PREFIX;
import static com.starter.telegram.listener.query.BillCallbackExecutor.CONFIRM_BILL_PREFIX;
import static com.starter.telegram.listener.query.BillCallbackExecutor.SKIP_BILL_PREFIX;
import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.QUERY_SEPARATOR;
import static com.starter.telegram.listener.query.CallbackQueryUpdateListener.RECOGNIZE_BILL_PREFIX;
import static com.starter.telegram.service.TelegramBotService.latestKeyboard;
import static com.starter.telegram.service.TelegramStatsService.AVAILABLE_UNITS;
import static com.starter.telegram.service.TelegramStatsService.STATS_CALLBACK_QUERY_PREFIX;
import static com.starter.telegram.service.render.TelegramStaticRenderer.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageRenderer {
    private static final String START_COMMAND_TEMPLATE = "start.txt";
    private static final String ADD_ME_TEMPLATE = "add_me.txt";
    private static final String ADD_ME_APPROVED_TEMPLATE = "add_me_approved.txt";
    private static final String NEW_BILL_TEMPLATE = "new_bill.txt";
    private static final String BILL_TEMPLATE = "bill.txt";
    private static final String DAILY_REMINDER_TEMPLATE = "daily.txt";
    private static final String WEEKLY_REPORT_TEMPLATE = "weekly.txt";
    private static final String NO_BILLS_TEMPLATE = "no_bills.txt";
    private static final String STATS_TEMPLATE = "stats.txt";
    private static final String LATEST_BILLS_TEMPLATE = "latest_bills.txt";
    private static final String BILL_CONFIRMED_TEMPLATE = "#amount# confirmed. <a href='#edit_url#'>Edit</a>";
    private static final String STAT_ENTRY_TEMPLATE = "◾\uFE0F #first#  <b>#second#</b>";
    private static final String TOP_EXPENSE_TEMPLATE = "<i>#first#</i>  <b>#second#</b>";
    private static final String FILE_RECEIVED_TEMPLATE = "File received. Processing...\n\n#beta#";

    private final TemplateReader templateReader;

    private final ServerProperties serverProperties;

    private final CurrenciesService currenciesService;

    private final BetaFeaturesProperties betaFeaturesProperties;

    public SendMessage renderBill(Long chatId, Bill bill, boolean spoiler) {
        final var caption = renderCaption(bill, spoiler);
        final var keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("\uD83D\uDDD1 Skip").callbackData(SKIP_BILL_PREFIX + bill.getId()),
                renderWebAppButton("✏\uFE0F Edit", "bill", bill.getId().toString()),
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
                        + QUERY_SEPARATOR + group.getChatId())
        );
        return linkPreviewOff(new SendMessage(owner.getTelegramChatId(), textPart), keyboard);
    }

    public BaseRequest<?, ?> renderAddMeAcceptedUpdate(Long chatId, MaybeInaccessibleMessage message, UserInfo userInfo, Group group) {
        final var textPart = templateReader.read(ADD_ME_APPROVED_TEMPLATE)
                .replaceAll("#edit_url#", renderWebAppDirectUrl("group", group.getId()))
                .replaceAll("#user_name#", renderTelegramUsername(userInfo))
                .replaceAll("#group_name#", group.getTitle());
        return tryUpdateMessage(chatId, message, textPart);
    }

    public SendMessage renderSettings(Long chatId) {
        return new SendMessage(chatId, "⚙\uFE0F Bot settings ")
                .replyMarkup(new InlineKeyboardMarkup(renderWebAppButton("View and edit", "settings", "")));
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
                .replace("#example#", renderExample());
        return new SendMessage(chatId, textPart)
                .replyMarkup(
                        new InlineKeyboardMarkup(
                                new InlineKeyboardButton[]{new InlineKeyboardButton("⌨\uFE0F OK, recognize my bill")
                                        .callbackData(RECOGNIZE_BILL_PREFIX)},
                                new InlineKeyboardButton[]{
                                        renderWebAppButton("\uD83E\uDDFE I'll use the form", "bill", "new")}
                        ))
                .parseMode(ParseMode.HTML);
    }

    public SendMessage renderStartMessage(Long chatId, String firstName) {
        final var textPart = templateReader.read(START_COMMAND_TEMPLATE)
                .replace("#name#", StringUtils.hasText(firstName) ? firstName : "Anonymous")
                .replace("#example#", renderExample())
                .replace("#beta#", renderDocumentsBeta(betaFeaturesProperties));
        return new SendMessage(chatId, textPart).replyMarkup(latestKeyboard()).parseMode(ParseMode.HTML);
    }

    public BaseRequest<?, ?> renderStats(Long chatId,
                                         String timeRangeText,
                                         Map<String, Double> totals,
                                         MaybeInaccessibleMessage previousMessage) {
        final var keyboard = renderStatsKeyboard();
        if (totals.isEmpty()) {
            return renderNoBills(chatId, timeRangeText, keyboard, previousMessage);
        }
        final var stats = totals.entrySet().stream()
                .map(entry -> {
                    final var currency = entry.getKey();
                    final var amount = renderAmount(entry.getValue(), currenciesService.getSymbol(currency));
                    return STAT_ENTRY_TEMPLATE
                            .replaceAll("#first#", currency)
                            .replaceAll("#second#", amount);
                })
                .collect(Collectors.joining("\n"));
        final var textPart = templateReader.read(STATS_TEMPLATE)
                .replace("#time_range#", timeRangeText)
                .replace("#stats#", stats);
        return tryUpdateMessage(chatId, previousMessage, textPart, keyboard.inlineKeyboard());
    }

    public SendMessage renderLatestBills(Long chatId, Page<Bill> lastBills, String group) {
        final var keyboard = renderStatsKeyboard();
        final var bills = lastBills.stream()
                .map(bill -> STAT_ENTRY_TEMPLATE
                        .replaceAll("#first#", bill.getPurpose())
                        .replaceAll("#second#", renderAmount(bill.getAmount(), currenciesService.getSymbol(bill.getCurrency()))))
                .collect(Collectors.joining("\n"));
        final var textPart = templateReader.read(LATEST_BILLS_TEMPLATE)
                .replace("#num#", "" + lastBills.getSize())
                .replace("#group_name#", group)
                .replace("#bills#", bills);
        return new SendMessage(chatId, textPart)
                .replyMarkup(keyboard)
                .parseMode(ParseMode.HTML);
    }

    private BaseRequest<?, ?> renderNoBills(Long chatId,
                                            String timeRange,
                                            InlineKeyboardMarkup keyboard,
                                            MaybeInaccessibleMessage previousMessage) {
        final var textPart = templateReader.read(NO_BILLS_TEMPLATE)
                .replace("#time_range#", timeRange)
                .replace("#example#", renderExample());
        return tryUpdateMessage(chatId, previousMessage, textPart, keyboard.inlineKeyboard());
    }

    public SendMessage renderWeeklyReport(Long chatId,
                                          Double totalSpend,
                                          TagAmount topTag,
                                          String topTagCurrency,
                                          Bill maxSpend,
                                          Bill minSpend,
                                          String firstName,
                                          boolean silentMode) {
        final var keyboard = new InlineKeyboardMarkup(renderWebAppButton("View all stats", "dashboard", ""));
        final var totalSpendText = renderAmount(totalSpend, currenciesService.getSymbol(topTagCurrency));
        final var maxSpendText = TOP_EXPENSE_TEMPLATE
                .replaceAll("#first#", maxSpend.getPurpose())
                .replaceAll("#second#", renderAmount(maxSpend.getAmount(), currenciesService.getSymbol(maxSpend.getCurrency())));
        final var minSpendText = TOP_EXPENSE_TEMPLATE
                .replaceAll("#first#", minSpend.getPurpose())
                .replaceAll("#second#", renderAmount(minSpend.getAmount(), currenciesService.getSymbol(minSpend.getCurrency())));
        final var text = templateReader.read(WEEKLY_REPORT_TEMPLATE)
                .replace("#name#", firstName)
                .replace("#total#", totalSpendText)
                .replace("#top_tag#", topTag.getName())
                .replaceAll("#top_tag_amount#", renderAmount(topTag.getAmount(), currenciesService.getSymbol(topTagCurrency)))
                .replace("#max_spend#", maxSpendText)
                .replace("#min_spend#", minSpendText);
        return new SendMessage(chatId, text)
                .replyMarkup(keyboard)
                .parseMode(ParseMode.HTML)
                .disableNotification(silentMode);
    }

    public SendMessage renderGroups(Long chatId, List<Pair<Group, Long>> groups) {
        final var textPart = TelegramStaticRenderer.renderGroups(groups);
        final var keyboard = new InlineKeyboardMarkup(
                renderWebAppButton("Manage groups", "groups", "")
        );
        return new SendMessage(chatId, textPart)
                .replyMarkup(keyboard)
                .parseMode(ParseMode.HTML);
    }

    public SendMessage renderFileReceivedNotice(Long chatId) {
        final var textPart = FILE_RECEIVED_TEMPLATE.replace("#beta#", renderDocumentsBeta(betaFeaturesProperties));
        return new SendMessage(chatId, textPart).parseMode(ParseMode.HTML);
    }

    public SendMessage renderChatWithBillsUsage(Long chatId) {
        final var textPart = "Usage: <code>/chat How much did I spend on this week?</code>\n\n";
        return new SendMessage(chatId, textPart + renderChatWithBillsBeta(betaFeaturesProperties))
                .parseMode(ParseMode.HTML);
    }

    public SendMessage renderHelp(Long chatId) {
        return new SendMessage(chatId, "Facing any issue? Contact us at @ai_brozz\n\nOr use the form bellow:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(new InlineKeyboardMarkup(
                        renderWebAppButton("Get help or post feedback", "help?chatId=" + chatId, "")
                ));
    }

    public InlineKeyboardMarkup renderStatsKeyboard() {
        final var keyboard = new InlineKeyboardMarkup();
        final var firstRow = AVAILABLE_UNITS
                .keySet()
                .stream()
                .map(key -> new InlineKeyboardButton(AVAILABLE_UNITS.get(key))
                        .callbackData(STATS_CALLBACK_QUERY_PREFIX + key.name()))
                .toArray(InlineKeyboardButton[]::new);
        keyboard.addRow(firstRow);
        keyboard.addRow(renderWebAppButton("View all stats", "dashboard", ""));
        return keyboard;
    }

    private InlineKeyboardButton renderWebAppButton(String text, String path, String pathVariable) {
        return new InlineKeyboardButton(text).webApp(renderWebApp(path, pathVariable));
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