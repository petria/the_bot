package org.freakz.ui.back.repository;


import org.freakz.ui.back.models.ERole;
import org.freakz.ui.back.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}
