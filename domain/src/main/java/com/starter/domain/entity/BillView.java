package com.starter.domain.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Immutable
@Entity
@Subselect("SELECT * FROM bills_view")
@Synchronize({"bills, bills_to_tags, bill_tags, groups"})
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class BillView {

    @Id
    private UUID id;
    private Instant mentionedDate;
    private Double amount;
    private String currency;
    private String purpose;
    private String tags;
    private UUID ownerId;
}
