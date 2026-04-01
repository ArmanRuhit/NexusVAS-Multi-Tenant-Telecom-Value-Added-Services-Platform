package dev.armanruhit.nexusvas.auth.domain.repository;

import dev.armanruhit.nexusvas.auth.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameAndTenantIdIsNull(String name);

    Optional<Role> findByNameAndTenantId(String name, String tenantId);

    List<Role> findByTenantId(String tenantId);

    List<Role> findByIsSystemRoleTrue();
}
