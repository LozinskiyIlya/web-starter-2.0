package com.starter.domain.repository;

import com.starter.domain.entity.AbstractEntity;
import com.starter.domain.entity.AbstractEntity.State;
import jakarta.persistence.Table;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
abstract class AbstractRepositoryTest<E extends AbstractEntity> {

    @Autowired
    private Repository<E> repository;

    @Autowired
    protected TransactionTemplate template;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    abstract E createEntity();

    void checkCreatedEntity(E created) {

    }

    @SneakyThrows
    private String getTableName() {
        var name = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName();
        var klass = Class.forName(name);
        return Arrays.stream(klass.getAnnotations())
                .filter(it -> it.annotationType() == Table.class)
                .map(it -> (Table) it)
                .map(Table::name)
                .findFirst()
                .orElseThrow();
    }

    void checkAreEqual(E persisted, E requested) {
        var getters = new HashSet<Method>();
        Class<?> current = persisted.getClass();
        while (current.getSuperclass() != null) {
            final var methods = current.getMethods();
            Arrays.stream(methods)
                    .filter(it -> it.getParameterCount() == 0)
                    .filter(it -> it.getName().startsWith("get"))
                    .forEach(getters::add);
            current = current.getSuperclass();
            getters.forEach(g -> compareGetterInvocationResults(g, persisted, requested));
        }
    }

    @SneakyThrows
    private void compareGetterInvocationResults(Method getter, E persisted, E requested) {
        getter.setAccessible(true);
        final var persistedResult = getter.invoke(persisted);
        final var requestedResult = getter.invoke(requested);
        if (Collection.class.isAssignableFrom(getter.getReturnType())) {
            var p = (Collection) persistedResult;
            var r = (Collection) requestedResult;
            assertTrue(p.size() == r.size() && p.containsAll(r) && r.containsAll(p), String.format("%s collections %s and %s should be equal", getter.getName(), persistedResult, requestedResult));
        } else if (Map.class.isAssignableFrom(getter.getReturnType())) {
            throw new UnsupportedOperationException("Not implemented yet");
        } else if (Instant.class.isAssignableFrom(getter.getReturnType())) {
            var p = (Instant) persistedResult;
            var r = (Instant) requestedResult;
            if (p == null) {
                assertNull(r);
            } else {
                assertTrue(p.toEpochMilli() - r.toEpochMilli() <= 1, String.format("%s instants: [%s], [%s] should be equal", getter.getName(), p, r));
            }
        } else if (getter.getReturnType().isArray()) {
            if (persistedResult instanceof float[] && requestedResult instanceof float[]) {
                var p = (float[]) persistedResult;
                var r = (float[]) requestedResult;
                assertArrayEquals(p, r, String.format("%s arrays %s and %s should be equal",
                        getter.getName(),
                        Arrays.toString(p),
                        Arrays.toString(r)));
            } else {
                throw new UnsupportedOperationException(String.format("%s[] comparison not yet implemented", getter.getReturnType().getComponentType()));
            }

        } else {
            assertEquals(persistedResult, requestedResult, String.format("%s invocation results should be equal", getter.getName()));
        }
    }

    @Nested
    @DisplayName("While persisting entity")
    class WhilePersistingEntity {

        private final AtomicReference<UUID> entityId = new AtomicReference<>();

        @Test
        @DisplayName("can be persisted")
        void canBeSaved() {
            var saved = template.execute(t -> {
                        final var entity = createEntity();
                        checkCreatedEntity(entity);
                        return entity;
                    }
            );
            entityId.set(saved.getId());
            template.executeWithoutResult(t -> {
                var queried = repository.findById(saved.getId());
                assertTrue(queried.isPresent());
                checkAreEqual(saved, queried.get());
            });
        }

        @AfterEach
        void clearRepo() {
            if (entityId.get() != null && repository.findById(entityId.get()).isPresent())
                repository.deleteById(entityId.get());
        }

    }

    @Nested
    @DisplayName("While deleting entity")
    class WhileDeletingAnEntity {

        @Test
        @DisplayName("can be deleted by entity object")
        void canBeDeletedByObject() {
            var entityId = new AtomicReference<UUID>();
            var saved = template.execute(t -> createEntity());
            entityId.set(saved.getId());
            template.executeWithoutResult(t -> {
                var queried = repository.findById(saved.getId());
                repository.delete(queried.get());
            });
            assertFalse(repository.findById(entityId.get()).isPresent());
            assertFalse(repository.existsById(entityId.get()));
        }

        @Test
        @DisplayName("can be deleted by id")
        void canBeDeletedById() {
            var entityId = new AtomicReference<UUID>();
            var saved = template.execute(t -> createEntity());
            entityId.set(saved.getId());
            template.executeWithoutResult(t -> repository.deleteById(entityId.get()));
            assertFalse(repository.findById(entityId.get()).isPresent());
            assertFalse(repository.existsById(entityId.get()));
        }

        @Test
        @DisplayName("still exists in db")
        void stillExistsInDb() {
            var entityId = new AtomicReference<UUID>();
            var saved = template.execute(t -> createEntity());
            entityId.set(saved.getId());
            template.executeWithoutResult(t -> repository.deleteById(entityId.get()));
            final var params = new MapSqlParameterSource()
                    .addValue("id", saved.getId());
            var state = jdbcTemplate.queryForObject("select state from " + getTableName() + " where id=:id", params, State.class);
            assertEquals(State.DELETED, state);
        }

    }

}
