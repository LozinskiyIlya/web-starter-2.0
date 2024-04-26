package com.starter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.List;


@Getter
@Setter
@Entity
@Table(name = "groups",
        indexes = {
                @Index(name = "group_chat_id_index", columnList = "chat_id"),
                @Index(name = "group_owner_fk_index", columnList = "owner_id")
        }
)
@SQLDelete(sql = "UPDATE groups SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedGroupsById")
@NamedQuery(name = "findNonDeletedGroupsById", query = "SELECT g FROM Group g WHERE g.id = ?1 AND g.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class Group extends AbstractEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @NotNull
    @Column(name = "chat_id", unique = true, nullable = false, updatable = false)
    private Long chatId;

    @NotNull
    private String title;

    @ManyToMany
    @JoinTable(name = "group_members",
            joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
            inverseJoinColumns = {
                    @JoinColumn(name = "member_id", referencedColumnName = "id")
            },
            uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "member_id"}),
            indexes = {@Index(columnList = "group_id"), @Index(columnList = "member_id")}
    )
    private List<User> members;

    public boolean contains(User user) {
        return members.contains(user);
    }
}
