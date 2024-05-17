package com.starter.web.dto;


import lombok.Data;

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
    }
}
