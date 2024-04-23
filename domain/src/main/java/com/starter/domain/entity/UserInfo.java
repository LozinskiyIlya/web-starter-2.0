package com.starter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;


@Getter
@Setter
@Entity
@Table(name = "user_infos",
        indexes = {
                @Index(name = "user_info_user_fk_index", columnList = "user_id"),
                @Index(name = "user_info_telegram_chat_id_index", columnList = "telegram_chat_id")
        })
@SQLDelete(sql = "UPDATE user_infos SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedUserInfosById")
@NamedQuery(name = "findNonDeletedUserInfosById", query = "SELECT u FROM UserInfo u WHERE u.id = ?1 AND u.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class UserInfo extends AbstractEntity {

    @NotNull
    @Column(name = "first_name")
    private String firstName = "Unknown";

    @NotNull
    @Column(name = "last_name")
    private String lastName = "Unknown";

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false, updatable = false)
    private User user;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
    private String language = "en";
    private Boolean isTelegramPremium = false;
    private String telegramUsername;

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
