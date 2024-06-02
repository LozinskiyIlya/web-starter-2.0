package com.starter.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSettingsDto {
    @NotNull
    private Boolean autoConfirmBills;
    @NotNull
    private Boolean spoilerBills;
    @NotNull
    private Boolean pinCodeEnabled;
    private String pinCode;
    private String lastUpdatedAt;
}
