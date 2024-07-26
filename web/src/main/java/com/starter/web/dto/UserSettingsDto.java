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
    @NotNull
    private String timezone;
    @NotNull
    private Boolean silentMode;
    @NotNull
    private Boolean dailyReminder;
    @NotNull
    private Boolean weeklyReport;
    @NotNull
    private String dailyReminderAt;
    private String pinCode;
    private String lastUpdatedAt;
}
