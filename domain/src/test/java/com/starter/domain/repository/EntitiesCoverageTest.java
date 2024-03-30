package com.starter.domain.repository;

import com.starter.domain.entity.AbstractEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.reflections.Reflections;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntitiesCoverageTest {

    @TestFactory
    @DisplayName("All entities should be covered with integration tests")
    Stream<DynamicTest> allShouldBeCovered() {
        var entityReflections = new Reflections("com.starter.domain.entity");
        var subTypesOfEntity = entityReflections.getSubTypesOf(AbstractEntity.class);
        assertFalse(subTypesOfEntity.isEmpty(), "Some entities should exist. Fix the scanning packages");
        var repoReflections = new Reflections("com.starter.domain.repository");
        var testClassNames = repoReflections.getSubTypesOf(AbstractRepositoryTest.class)
                .stream()
                .map(it -> it.getSimpleName())
                .collect(Collectors.toSet());
        return subTypesOfEntity.stream()
                .map(it -> it.getSimpleName())
                .map(it -> DynamicTest.dynamicTest(it,
                        () -> {
                            final var testName = String.format("%sRepositoryIT", it);
                            assertTrue(testClassNames.contains(testName),
                                    String.format("Entity class: %s, should be covered with integration test. Add %sRepositoryIT", it, it));
                        }));
    }
}
