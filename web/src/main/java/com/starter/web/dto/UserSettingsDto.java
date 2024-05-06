package com.starter.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class UserSettingsDto {
    @NotNull
    private Boolean autoConfirmBills;
    @NotNull
    private Boolean spoilerBills;
    private String pinCode;
    private String lastUpdatedAt;
}
