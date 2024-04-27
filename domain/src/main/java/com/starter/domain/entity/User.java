package com.starter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * @author ilya
 * @date 26.08.2021
 */

@Getter
@Setter
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "user_login_index", columnList = "login"),
                @Index(name = "user_role_fk_index", columnList = "role_id")
        }
)
@SQLDelete(sql = "UPDATE users SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedUserById")
@NamedQuery(name = "findNonDeletedUserById", query = "SELECT u FROM User u WHERE u.id = ?1 AND u.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class User extends AbstractEntity {

    @NotNull
    @Column(unique = true)
    private String login;

    @NotNull
    private String password;

    @NotNull
    private Boolean firstLogin = true;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "user")
    private UserInfo userInfo;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "user")
    private UserSettings userSettings;

    @Column
    @Enumerated(EnumType.STRING)
    private UserType userType = UserType.TELEGRAM;

    public enum UserType {
        /**
         * Manually created
         */
        REAL,

        /**
         * Created by google auth
         */
        GOOGLE,

        /**
         * Created by telegram
         */
        TELEGRAM
    }

    public static User googleUser(String login) {
        var google = new User();
        google.setLogin(login);
        google.setPassword(UUID.randomUUID().toString());
        google.setUserType(UserType.GOOGLE);
        return google;
    }

    public static User randomPasswordUser(String login) {
        var random = new User();
        random.setLogin(login);
        random.setPassword(UUID.randomUUID().toString());
        random.setUserType(UserType.REAL);
        return random;
    }

    public static User randomPasswordTelegramUser(String login) {
        var random = new User();
        random.setLogin(login);
        random.setPassword(UUID.randomUUID().toString());
        random.setUserType(UserType.TELEGRAM);
        return random;
    }

}
