package com.starter.domain.repository;

import com.starter.domain.entity.BillView;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * @author ilya
 * @date 08.11.2021
 */
public interface BillViewRepository extends JpaRepository<BillView, UUID>, JpaSpecificationExecutor<BillView> {

//    static Specification<BillView> infoLike(String searchStringAnyCase) {
//        final var searchString = "%" + searchStringAnyCase.toLowerCase() + "%";
//        Specification<BillView> searchSpec = (root, query, criteriaBuilder) ->
//                criteriaBuilder.or(
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.FIRST_NAME)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.LAST_NAME)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.COMPANY)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.LOCATION)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.CONTACT_VALUES)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.ABOUT)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.SEARCH_FOR)), searchString),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get(BillView_.TAGS)), searchString)
//                );
//
//        Specification<BillView> fullNameSpec = (root, query, criteriaBuilder) -> {
//            final var firstNameAndSpace = criteriaBuilder.concat(root.get(BillView_.FIRST_NAME), " ");
//            final var lastNameAndSpace = criteriaBuilder.concat(root.get(BillView_.LAST_NAME), " ");
//            final var fullName = criteriaBuilder.concat(firstNameAndSpace, root.get(BillView_.LAST_NAME));
//            final var fullNameReversed = criteriaBuilder.concat(lastNameAndSpace, root.get(BillView_.FIRST_NAME));
//            return criteriaBuilder.or(
//                    criteriaBuilder.like(criteriaBuilder.lower(fullName), searchString),
//                    criteriaBuilder.like(criteriaBuilder.lower(fullNameReversed), searchString)
//            );
//        };
//        return searchSpec.or(fullNameSpec);
//    }
//
//    static Specification<BillView> byCommunity(UUID communityId) {
//        return (root, query, criteriaBuilder) ->
//                criteriaBuilder.equal(root.get(BillView_.COMMUNITY_ID), communityId);
//    }
    
}
