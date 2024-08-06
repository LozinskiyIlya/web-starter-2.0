package com.starter.domain.repository;

import com.starter.domain.entity.BillView;
import com.starter.domain.entity.BillView_;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */
public interface BillViewRepository extends JpaRepository<BillView, UUID>, JpaSpecificationExecutor<BillView> {

    static Specification<BillView> infoLike(String searchStringAnyCase) {
        final var searchString = "%" + searchStringAnyCase.toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.PURPOSE)), searchString),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.TAGS)), searchString)
                );
    }

    static Specification<BillView> byOwnerId(UUID ownerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(BillView_.OWNER_ID), ownerId);
    }

    default Page<BillView> searchBills(UUID ownerId, String search, Pageable pageable) {
        final var spec = byOwnerId(ownerId).and(infoLike(search));
        return findAll(spec, pageable);
    }
}
