package com.starter.domain.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

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
}
