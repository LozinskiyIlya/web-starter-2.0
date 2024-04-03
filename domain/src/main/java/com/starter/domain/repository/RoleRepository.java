package com.starter.domain.repository;


import com.starter.domain.entity.Role;
import java.util.Optional;

/**
 * @author ilya
 * @date 08.11.2021
 */

public interface RoleRepository extends Repository<Role> {

    Optional<Role> findByName(String name);
}
