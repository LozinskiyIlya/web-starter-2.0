package com.starter.domain.repository;

import com.starter.domain.entity.AbstractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface Repository<T extends AbstractEntity> extends JpaRepository<T, UUID> {
}
