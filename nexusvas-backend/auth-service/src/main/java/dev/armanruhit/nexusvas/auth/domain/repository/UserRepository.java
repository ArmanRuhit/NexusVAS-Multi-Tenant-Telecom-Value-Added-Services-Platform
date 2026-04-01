package dev.armanruhit.nexusvas.auth.domain.repository;

import dev.armanruhit.nexusvas.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenantId(String email, String tenantId);

    Optional<User> findByEmail(String email);

    Optional<User> findByMsisdnAndTenantId(String msisdn, String tenantId);

    boolean existsByEmailAndTenantId(String email, String tenantId);

    @Query("SELECT u FROM User u JOIN FETCH u.roles r JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") UUID id);

    @Query("SELECT u FROM User u JOIN FETCH u.roles r JOIN FETCH r.permissions WHERE u.email = :email AND u.tenantId = :tenantId")
    Optional<User> findByEmailAndTenantIdWithPermissions(
            @Param("email") String email,
            @Param("tenantId") String tenantId);
}
