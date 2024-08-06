package com.starter.web.dto;


import com.starter.domain.entity.Bill;
import com.starter.domain.repository.BillRepository.TagAmount;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class GroupDto {
    private UUID id;
    private String title;
    private String defaultCurrency;
    private UUID ownerId;
    private long membersCount;
    private long billsCount;
    private GroupLastBillDto lastBill;
    private ChartDataDto chartData;

    @Data
    public static class GroupMemberDto {
        private UUID id;
        private String name;
    }

    @Data
    public static class GroupLastBillDto {
        private UUID id;
        private String purpose;
        private Double amount;
        private String currency;
        private String date;
        private Bill.BillStatus status;
    }

    @Data
    public static class ChartDataDto {
        private String currency;
        private String currencySymbol;
        private List<TotalDto> totals;
        private List<TagAmount> data;
    }

    @Data
    public static class TotalDto {
        private double total;
        private String currency;
    }

    @Data
    public static class GroupLightDto {
        private UUID id;
        private String title;
    }
}
