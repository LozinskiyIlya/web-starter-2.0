package com.starter.web.dto;


import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class GroupDto {
    private UUID id;
    private String title;
    private String owner;
    private List<String> members;
    private List<BillDto> bills;


    @Data
    public static class GroupLightDto {
        private UUID id;
        private String title;
    }
}
