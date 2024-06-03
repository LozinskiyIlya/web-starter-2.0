package com.starter.web.service;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.BillRepository.TagAmount;
import com.starter.web.controller.ChartsController.Timeline;
import com.starter.web.dto.GroupDto.TotalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartsService {

    private final BillRepository billRepository;

    public List<TagAmount> getAmountPerTag(List<Group> groups, Instant from, Instant to, String currency) {
        return billRepository.findTagAmountsByGroupInAndCurrency(groups, currency, from, to);
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

    public List<Timeline> getTimeline(List<Bill> bills, String selectedCurrency) {
        return bills.stream()
                .filter(bill -> selectedCurrency.equals(bill.getCurrency()))
                .collect(Collectors.groupingBy(bill ->
                                bill.getMentionedDate().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.summingDouble(Bill::getAmount)))
                .entrySet()
                .stream()
                .map(entry -> {
                    final var timeline = new Timeline();
                    timeline.setDate(entry.getKey().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    timeline.setAmount(entry.getValue());
                    return timeline;
                })
                .sorted(Comparator.comparing(Timeline::getDate))
                .toList();
    }
}
