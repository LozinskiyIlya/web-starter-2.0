package com.starter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Entity
@Table(name = "bills",
        indexes = {
                @Index(name = "bill_groups_fk_index", columnList = "group_id")
        })
@SQLDelete(sql = "UPDATE bills SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedBillsById")
@NamedQuery(name = "findNonDeletedBillsById", query = "SELECT b FROM Bill b WHERE b.id = ?1 AND b.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class Bill extends AbstractEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToMany
    @JoinTable(name = "bills_to_tags",
            joinColumns = @JoinColumn(name = "bill_id", referencedColumnName = "id"),
            inverseJoinColumns = {
                    @JoinColumn(name = "tag_id", referencedColumnName = "id")
            },
            uniqueConstraints = @UniqueConstraint(columnNames = {"bill_id", "tag_id"}),
            indexes = {@Index(columnList = "bill_id"), @Index(columnList = "tag_id")}
    )
    private Set<BillTag> tags = new HashSet<>();

    @NotNull
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BillStatus status = BillStatus.NEW;

    private String buyer;
    private String seller;
    private String purpose;
    private String currency;
    private Double amount;
    private Instant mentionedDate;
    private Integer messageId;
    public enum BillStatus {
        NEW, SENT, CONFIRMED
    }
}
