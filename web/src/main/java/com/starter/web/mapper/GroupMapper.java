package com.starter.web.mapper;

import com.starter.common.service.CurrenciesService;
import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.UserInfo;
import com.starter.domain.repository.BillRepository;
import com.starter.domain.repository.GroupRepository;
import com.starter.web.dto.GroupDto;
import com.starter.web.dto.GroupDto.GroupLastBillDto;
import com.starter.web.dto.GroupDto.GroupMemberDto;
import com.starter.web.service.ChartsService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.starter.domain.entity.Bill.DEFAULT_CURRENCY;


@Component
@RequiredArgsConstructor
public class GroupMapper {

    private final StaticGroupMapper staticMapper;
    private final BillRepository billRepository;
    private final GroupRepository groupRepository;
    private final CurrenciesService currenciesService;
    private final ChartsService chartsService;

    public GroupDto toDto(Group group) {
        final var dto = staticMapper.toDto(
                group,
                billRepository.findFirstNotSkippedByGroupOrderByMentionedDateDesc(group),
                groupRepository.countMembers(group),
                billRepository.countNotSkippedByGroup(group)
        );
        enrichWithChartData(group, dto);
        return dto;
    }

    private void enrichWithChartData(Group group, GroupDto dto) {
        final var list = List.of(group);
        final var chartData = new GroupDto.ChartDataDto();
        // Set total amount for each currency
        chartData.setTotals(chartsService.getTotalsByGroups(list));
        // In group view we show chart only for the default or the most used currency
        final var currency = selectCurrency(group, chartData.getTotals());
        chartData.setCurrency(currency);
        chartData.setCurrencySymbol(currenciesService.getSymbol(currency));
        // Now for that currency select amount per tag
        final var minDate = Instant.EPOCH;
        final var maxDate = Instant.now();
        final var data = chartsService.getAmountPerTag(list, minDate, maxDate, currency);
        chartData.setData(data);
        dto.setChartData(chartData);
    }

    public GroupMemberDto toGroupMemberDto(UserInfo userInfo) {
        return staticMapper.toGroupMemberDto(userInfo);
    }

    private String selectCurrency(Group group, List<GroupDto.TotalDto> totals) {
        // Check for the default currency in the group, or find the most used currency if not present
        String currency = Optional.ofNullable(group.getDefaultCurrency())
                .orElseGet(() -> billRepository.findMostUsedCurrencyByGroup(group));

        // Check if the selected currency is present in the totals list
        boolean currencyExistsInTotals = totals.stream()
                .map(GroupDto.TotalDto::getCurrency)
                .anyMatch(currency::equals);

        // If the currency is not found in the totals list, use the currency from the first total
        if (!currencyExistsInTotals) {
            currency = totals.stream()
                    .findFirst()
                    .map(GroupDto.TotalDto::getCurrency)
                    .orElse(DEFAULT_CURRENCY);
        }

        return currency;
    }


    @Mapper(componentModel = "spring")
    interface StaticGroupMapper {

        @Mapping(target = "ownerId", source = "group.owner.id")
        @Mapping(target = "id", source = "group.id")
        GroupDto toDto(Group group, Bill lastBill, long membersCount, long billsCount);

        @Mapping(target = "name", expression = "java(userInfo.getFullName())")
        @Mapping(target = "id", source = "userInfo.user.id")
        GroupMemberDto toGroupMemberDto(UserInfo userInfo);

        @Mapping(target = "date", source = "mentionedDate")
        GroupLastBillDto toLastBillDto(Bill lastBill);
    }
}



