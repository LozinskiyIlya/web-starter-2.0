package com.starter.domain.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */

@Getter
@Setter
@Entity
@Table(name = "api_actions", indexes = {
        @Index(name = "api_action_user_index", columnList = "user_id"),
})
@SQLDelete(sql = "UPDATE api_actions SET state='DELETED' WHERE id=?")
@Loader(namedQuery = "findNonDeletedApiActionById")
@NamedQuery(name = "findNonDeletedApiActionById", query = "SELECT a FROM ApiAction a WHERE a.id = ?1 AND a.state <> 'DELETED'")
@Where(clause = "state != 'DELETED'")
public class ApiAction extends AbstractEntity {

    @Column(name = "user_id")
    private UUID userId;

    private String userQualifier;

    private String path;

    private String error;

    @NotNull
    private Instant executedAt = Instant.now();

    @NotNull
    @Column(columnDefinition = "TEXT")
    @Convert(converter = MetadataConverter.class)
    private Metadata metadata;

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Metadata {
        private String ip;
        private String userAgent;
        private String httpMethod;
        private String params;
    }

    static class MetadataConverter implements AttributeConverter<Metadata, String> {

        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        @SneakyThrows
        public String convertToDatabaseColumn(Metadata attribute) {
            return null == attribute ? null : mapper.writeValueAsString(attribute);
        }

        @Override
        @SneakyThrows
        public Metadata convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.equals("null") || dbData.equals("")) {
                return null;
            }
            return mapper.readValue(dbData, Metadata.class);
        }
    }
}
