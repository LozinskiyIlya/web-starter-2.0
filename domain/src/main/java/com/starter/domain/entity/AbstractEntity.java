package com.starter.domain.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * @author ilya
 * @date 26.08.2021
 */
@Getter
@Setter
@MappedSuperclass
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
public class AbstractEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column
    @Enumerated(EnumType.STRING)
    private State state = State.ACTIVE;

    public enum State {
        ACTIVE, DELETED
    }

}
