package dev.armanruhit.nexusvas.auth.security;

import dev.armanruhit.nexusvas.auth.domain.entity.User;
import dev.armanruhit.nexusvas.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return buildUserDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByEmailAndTenant(String email, String tenantId) {
        User user = userRepository.findByEmailAndTenantIdWithPermissions(email, tenantId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        var authorities = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(perm -> new SimpleGrantedAuthority("PERMISSION_" + perm.getName()))
            .collect(Collectors.toSet());

        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .forEach(authorities::add);

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
            .authorities(authorities)
            .accountLocked(user.isAccountLocked())
            .disabled(user.getStatus() == User.UserStatus.DELETED)
            .build();
    }
}
