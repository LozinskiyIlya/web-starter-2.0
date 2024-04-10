package com.starter.domain.configuration;

import com.starter.domain.entity.AbstractEntity;
import com.starter.domain.repository.Repository;
import com.starter.domain.repository.testdata.GroupTestDataCreator;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackageClasses = AbstractEntity.class)
@EnableJpaRepositories(basePackageClasses = Repository.class)
@ComponentScan(basePackageClasses = GroupTestDataCreator.class)
@Import(LiquibaseMigrationConfiguration.class)
public class DomainAutoconfiguration {
}
