package org.freakz.springboot.ui.backend.repository;

import org.freakz.springboot.ui.backend.models.ERole;
import org.freakz.springboot.ui.backend.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
