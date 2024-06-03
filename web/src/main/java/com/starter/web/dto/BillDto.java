package com.starter.web.dto;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.BillTag;
import com.starter.web.dto.GroupDto.GroupLightDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class BillDto {
    private UUID id;
    @NotBlank(message = "Purpose is required")
    private String purpose;
    @NotNull(message = "Amount is required")
    private Double amount;
    @NotBlank(message = "Currency is required")
    private String currency;
    private String buyer;
    private String seller;
    private String date;
    private String createdAt;
    private Bill.BillStatus status;
    private Set<BillTagDto> tags;
    private GroupLightDto group;

    @Data
    public static class BillTagDto {
        private UUID id;
        private String name;
        private String hexColor;
        private BillTag.TagType tagType;
    }
}


