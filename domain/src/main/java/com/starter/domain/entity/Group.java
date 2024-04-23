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
@Table(name = "groups",
        indexes = {
                @Index(name = "group_owner_fk_index", columnList = "owner_id"),
                @Index(name = "group_chat_id_index", columnList = "chat_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "group_owner_chat_id_unique", columnNames = {"owner_id", "chat_id"})
        }
)
@SQLDelete(sql = "UPDATE groups SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedGroupsById")
@NamedQuery(name = "findNonDeletedGroupsById", query = "SELECT g FROM Group g WHERE g.id = ?1 AND g.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class Group extends AbstractEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onwer_id", nullable = false, updatable = false)
    private User owner;

    @NotNull
    private String title;

    @NotNull
    @Column(name = "chat_id")
    private Long chatId;
}
