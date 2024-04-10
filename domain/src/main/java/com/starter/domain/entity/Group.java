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
@Table(name = "groups",
        indexes = {
                @Index(name = "group_user_fk_index", columnList = "user_id")
        })
@SQLDelete(sql = "UPDATE groups SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedGroupsById")
@NamedQuery(name = "findNonDeletedGroupsById", query = "SELECT g FROM Group g WHERE g.id = ?1 AND g.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class Group extends AbstractEntity {

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    private String name;

    @NotNull
    private String chatId;

    @NotNull
    private Instant createdAt = Instant.now();
}
