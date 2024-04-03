package com.starter.domain.repository;

import com.starter.domain.entity.ApiAction;

import java.util.List;
import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */
public interface ApiActionRepository extends Repository<ApiAction> {
    List<ApiAction> findAllByUserId(UUID userId);
}
