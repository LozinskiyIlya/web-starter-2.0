package com.starter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

/**
 * @author ilya
 * @date 26.08.2021
 */

@Getter
@Setter
@Entity
@Table(name = "subscriptions",
        indexes = {
                @Index(name = "subscription_user_fk_index", columnList = "user_id")
        }
)
@SQLDelete(sql = "UPDATE subscriptions SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedSubscriptionById")
@NamedQuery(name = "findNonDeletedSubscriptionById", query = "SELECT s FROM Subscription s WHERE s.id = ?1 AND s.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class Subscription extends AbstractEntity {

    @NotNull
    @OneToOne
    private User user;

    @NotNull
    private Double price;

    @NotNull
    private String currency;

    @NotNull
    private Instant endsAt;

}
