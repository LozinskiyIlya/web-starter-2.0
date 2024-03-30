package com.starter.domain.repository;

import com.starter.domain.entity.User;
import com.starter.domain.entity.User_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * @author ilya
 * @date 08.11.2021
 */
public interface UserRepository extends Repository<User>, JpaSpecificationExecutor<User> {

    default Optional<User> findByLogin(String login) {
        Specification<User> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(User_.LOGIN), login.toLowerCase());
        return this.findOne(specification);
    }

    @Override
    default User save(User user) {
        user.setLogin(user.getLogin().toLowerCase());
        return this.saveAndFlush(user);
    }
}
