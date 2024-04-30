package com.starter.web.dto;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class BillDto {
    private UUID id;
    private String buyer;
    private String seller;
    private String purpose;
    private Double amount;
    private String currency;
    private String date;
    private Bill.BillStatus status;
    private Set<BillTagDto> tags;

    @Data
    public static class BillTagDto {
        private UUID id;
        private String name;
        private String hexColor;
        private BillTag.TagType tagType;
    }
}


