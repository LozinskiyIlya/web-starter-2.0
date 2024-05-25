package com.starter.web.service;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillRepository.TagAmount;
import com.starter.web.controller.ChartsController.CurrencyByWeek;
import com.starter.web.dto.GroupDto.TotalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartsService {

    private final BillRepository billRepository;

    public List<TagAmount> getAmountPerTag(List<Group> groups, String currency) {
        return billRepository.findTagAmountsByGroupInAndCurrency(groups, currency);
    }

    public List<TotalDto> getTotalsByGroups(List<Group> groups) {
        return getTotals(billRepository.findAllNotSkippedByGroupIn(groups, Pageable.unpaged()).toList());
    }

    public List<TotalDto> getTotals(List<Bill> bills) {
        return bills.stream()
                .collect(Collectors.groupingBy(Bill::getCurrency, Collectors.summingDouble(Bill::getAmount)))
                .entrySet()
                .stream()
                .map(entry -> {
                    final var total = new TotalDto();
                    total.setCurrency(entry.getKey());
                    total.setTotal(entry.getValue());
                    return total;
                })
                .toList();
    }

    public List<String> getCurrencies(List<Group> groups) {
        return billRepository.findMostUsedCurrenciesByGroupIn(groups, Pageable.unpaged());
    }

    public List<CurrencyByWeek> getCurrencyByWeek(List<Bill> bills) {
        return bills.stream()
                .collect(Collectors.groupingBy(Bill::getCurrency, // Group by currency
                        Collectors.groupingBy(bill -> getStartOfWeek(bill.getMentionedDate())))) // Group by the start of the week
                .entrySet().stream()
                .map(currencyEntry -> {
                    CurrencyByWeek currencyByWeek = new CurrencyByWeek();
                    currencyByWeek.setCurrency(currencyEntry.getKey());
                    currencyByWeek.setByWeek(currencyEntry.getValue().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue().size()))); // Count the bills in each week
                    return currencyByWeek;
                })
                .collect(Collectors.toList());
    }

    private static Instant getStartOfWeek(Instant date) {
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        ZonedDateTime startOfWeek = zonedDateTime.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .truncatedTo(ChronoUnit.DAYS);
        return startOfWeek.toInstant();
    }
}
