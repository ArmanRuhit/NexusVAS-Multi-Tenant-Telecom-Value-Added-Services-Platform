package dev.armanruhit.nexusvas.auth.service;

import dev.armanruhit.nexusvas.auth.domain.entity.User;
import dev.armanruhit.nexusvas.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RbacService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    private static final String PERMS_CACHE_PREFIX = "perms:";
    private static final int PERMS_CACHE_TTL_MINUTES = 10;

    @Transactional(readOnly = true)
    public Set<String> resolvePermissions(UUID userId) {
        String cacheKey = PERMS_CACHE_PREFIX + userId;

        @SuppressWarnings("unchecked")
        Set<String> cached = (Set<String>) objectRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findByIdWithRolesAndPermissions(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Set<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(perm -> perm.getName())
            .collect(Collectors.toSet());

        objectRedisTemplate.opsForValue().set(cacheKey, permissions, PERMS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        return permissions;
    }

    public boolean checkPermission(UUID userId, String resource, String action) {
        String requiredPermission = resource + ":" + action;
        Set<String> permissions = resolvePermissions(userId);
        return permissions.contains(requiredPermission);
    }

    public void invalidatePermissionCache(UUID userId) {
        objectRedisTemplate.delete(PERMS_CACHE_PREFIX + userId);
        log.debug("Invalidated permission cache for user: {}", userId);
    }
}
