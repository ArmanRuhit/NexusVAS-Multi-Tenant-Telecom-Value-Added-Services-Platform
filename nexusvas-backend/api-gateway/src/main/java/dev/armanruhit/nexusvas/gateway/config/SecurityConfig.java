package dev.armanruhit.nexusvas.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        ReactiveJwtGrantedAuthoritiesConverter authoritiesConverter =
            new ReactiveJwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("PERMISSION_");
        authoritiesConverter.setAuthoritiesClaimName("permissions");

        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                // Auth service endpoints are public
                .pathMatchers(
                    "/auth/api/v1/auth/login",
                    "/auth/api/v1/auth/token/api-key",
                    "/auth/api/v1/auth/token/refresh",
                    "/auth/api/v1/auth/subscriber/otp/send",
                    "/auth/api/v1/auth/subscriber/otp/verify",
                    "/auth/.well-known/jwks.json",
                    "/actuator/health"
                ).permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            )
            .build();
    }
}
