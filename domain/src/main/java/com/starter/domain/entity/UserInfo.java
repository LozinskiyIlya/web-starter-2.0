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
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"first_name", "last_name", "slug"})
        },
        indexes = {
                @Index(name = "user_info_user_fk_index", columnList = "user_id")
        })
@SQLDelete(sql = "UPDATE user_infos SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedUserInfosById")
@NamedQuery(name = "findNonDeletedUserInfosById", query = "SELECT u FROM UserInfo u WHERE u.id = ?1 AND u.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class UserInfo extends AbstractEntity {

    @NotNull
    @Column(name = "first_name")
    private String firstName;

    @NotNull
    @Column(name = "last_name")
    private String lastName;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false, updatable = false)
    private User user;

    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
