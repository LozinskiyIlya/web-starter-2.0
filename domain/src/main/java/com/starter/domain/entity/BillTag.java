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
@Table(name = "bill_tags", indexes = {
        @Index(name = "bill_tags_user_id_fk_index", columnList = "user_id")
}, uniqueConstraints =  {
        @UniqueConstraint(name = "bill_tags_name_user_id_unique", columnNames = {"name", "user_id", "tag_type"})
})
@SQLDelete(sql = "UPDATE bill_tags SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedBillTagsById")
@NamedQuery(name = "findNonDeletedBillTagsById", query = "SELECT t FROM BillTag t WHERE t.id = ?1 AND t.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class BillTag extends AbstractEntity {

    @NotNull
    private String name;

    private String hexColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    private BillTag.TagType tagType = TagType.DEFAULT;

    public enum TagType {
        DEFAULT, USER_DEFINED
    }
}

