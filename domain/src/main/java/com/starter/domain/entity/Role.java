package com.starter.domain.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;


/**
 * @author ilya
 * @date 08.11.2021
 */

@Getter
@Setter
@Entity
@Table(name = "roles")
@SQLDelete(sql = "UPDATE roles SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedRoleById")
@NamedQuery(name = "findNonDeletedRoleById", query = "SELECT r FROM Role r WHERE r.id = ?1 AND r.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class Role extends AbstractEntity {

    @NotNull
    private String name;

    public enum Roles {
        USER("USER"),
        ADMIN("ADMIN"),
        INTERNAL_ADMIN("INTERNAL_ADMIN");

        private final String name;

        Roles(String name) {
            this.name = name;
        }

        public String getRoleName() {
            return "ROLE_" + name;
        }

        public String getName() {
            return name;
        }
    }
}
