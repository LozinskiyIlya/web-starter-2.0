package com.starter.domain.configuration;

import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({LiquibaseProperties.class})
public class LiquibaseMigrationConfiguration {

    @Bean
    public InitializingBean liquibaseRunner(LiquibaseProperties liquibaseProperties, @Lazy DataSource dataSource) {
        return new LiquibaseRunner(liquibaseProperties, dataSource);
    }

    @RequiredArgsConstructor
    private static class LiquibaseRunner implements InitializingBean {

        private final LiquibaseProperties properties;
        private final DataSource dataSource;

        @Override
        public void afterPropertiesSet() throws Exception {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(this.properties.getChangeLog());
            liquibase.setClearCheckSums(this.properties.isClearChecksums());
            liquibase.setContexts(this.properties.getContexts());
            liquibase.setDefaultSchema(this.properties.getDefaultSchema());
            liquibase.setLiquibaseSchema(this.properties.getLiquibaseSchema());
            liquibase.setLiquibaseTablespace(this.properties.getLiquibaseTablespace());
            liquibase.setDatabaseChangeLogTable(this.properties.getDatabaseChangeLogTable());
            liquibase.setDatabaseChangeLogLockTable(this.properties.getDatabaseChangeLogLockTable());
            liquibase.setDropFirst(this.properties.isDropFirst());
            // NEVER use properties.isEnabled true -> it leads to liquibase run before spring JPA initialization
            liquibase.setShouldRun(true);
            liquibase.setLabelFilter(this.properties.getLabelFilter());
            liquibase.setChangeLogParameters(this.properties.getParameters());
            liquibase.setRollbackFile(this.properties.getRollbackFile());
            liquibase.setTestRollbackOnUpdate(this.properties.isTestRollbackOnUpdate());
            liquibase.setTag(this.properties.getTag());
            liquibase.afterPropertiesSet();
        }
    }
}

