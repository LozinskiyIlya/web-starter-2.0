package com.starter.web.controller;

import com.starter.common.service.CurrenciesService;
import com.starter.common.service.CurrentUserService;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.GroupDto.TotalDto;
import com.starter.web.service.ChartsService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.starter.domain.entity.Bill.DEFAULT_CURRENCY;
import static com.starter.domain.repository.BillRepository.TagAmount;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/charts")
@Schema(title = "Данные для графиков")
public class ChartsController {

    private final CurrentUserService currentUserService;
    private final ChartsService chartsService;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final CurrenciesService currenciesService;

    @GetMapping({"/{groupId}", ""})
    public ChartData getChartsData(
            @PathVariable(value = "groupId", required = false) UUID groupId,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to) {
        final var currentUser = currentUserService.getUser().orElseThrow();
        final var groups = Optional.ofNullable(groupId)
                .flatMap(groupRepository::findById)
                .map(List::of)
                .orElse(groupRepository.findByChatId(currentUser.getUserInfo().getTelegramChatId())
                        .map(List::of)
                        .orElse(groupRepository.findAllByOwner(currentUser)));
        final var fromDefault = from == null ? LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : from;
        final var toDefault = to == null ? Instant.now() : to;
        final var bills = billRepository.findAllNotSkippedByGroupInAndMentionedDateBetween(groups, fromDefault, toDefault, Pageable.unpaged()).toList();
        final var chartData = new ChartData();
        chartData.setTotals(chartsService.getTotals(bills));
        chartData.setCurrencies(chartsService.getCurrencies(groups));
        final var selectedCurrency = StringUtils.hasText(currency) ?
                currency :
                groups.stream().findFirst().map(Group::getDefaultCurrency)
                        .orElse(chartData.getCurrencies().stream().findFirst()
                                .orElse(DEFAULT_CURRENCY));
        chartData.setSelectedCurrency(selectedCurrency);
        chartData.setTimeline(chartsService.getTimeline(bills, selectedCurrency));
        chartData.setCurrencySymbol(currenciesService.getSymbol(selectedCurrency));
        chartData.setAmountByTag(chartsService.getAmountPerTag(groups, fromDefault, toDefault, selectedCurrency));
        return chartData;
    }


    @Data
    public static class ChartData {
        String selectedCurrency;
        String currencySymbol;
        List<String> currencies;
        List<Timeline> timeline;
        List<TagAmount> amountByTag;
        List<TotalDto> totals;
    }

    @Data
    public static class Timeline {
        private Instant date;
        private Double amount;
    }
}
