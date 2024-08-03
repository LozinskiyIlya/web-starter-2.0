package com.starter.domain.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "user_settings",
        indexes = {
                @Index(name = "user_settings_user_fk_index", columnList = "user_id"),
        })
@SQLDelete(sql = "UPDATE user_settings SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedUserSettingsById")
@NamedQuery(name = "findNonDeletedUserSettingsById", query = "SELECT u FROM UserSettings u WHERE u.id = ?1 AND u.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class UserSettings extends AbstractEntity {

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false, updatable = false)
    private User user;

    @NotNull
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt = Instant.now();

    @NotNull
    @Column(name = "daily_reminder_at", nullable = false)
    private LocalTime dailyReminderAt = LocalTime.of(21, 0);

    @NotNull
    private String timezone = "UTC";

    @NotNull
    private Boolean spoilerBills = true;

    @NotNull
    private Boolean autoConfirmBills = false;

    @NotNull
    private Boolean silentMode = false;

    @NotNull
    private Boolean dailyReminder = true;

    @NotNull
    private Boolean weeklyReport = true;

    @NotNull
    private Boolean pinCodeEnabled = false;

    @NotNull
    private Boolean skipZeros = true;

    private String pinCode;
}
