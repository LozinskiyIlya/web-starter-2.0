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


    @Data
    public static class GroupMemberDto{
        private UUID id;
        private String name;
    }
}
