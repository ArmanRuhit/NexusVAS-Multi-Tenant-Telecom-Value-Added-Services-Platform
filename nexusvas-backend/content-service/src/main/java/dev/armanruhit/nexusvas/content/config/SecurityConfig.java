package dev.armanruhit.nexusvas.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        ReactiveJwtGrantedAuthoritiesConverter authoritiesConverter = new ReactiveJwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("PERMISSION_");
        authoritiesConverter.setAuthoritiesClaimName("permissions");

        ReactiveJwtAuthenticationConverter jwtConverter = new ReactiveJwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                .pathMatchers(HttpMethod.GET, "/api/v1/delivery/content").authenticated()
                .pathMatchers(HttpMethod.GET, "/api/v1/content/**").hasAuthority("PERMISSION_CONTENT_READ")
                .pathMatchers(HttpMethod.POST, "/api/v1/content").hasAuthority("PERMISSION_CONTENT_CREATE")
                .pathMatchers(HttpMethod.PATCH, "/api/v1/content/**").hasAuthority("PERMISSION_CONTENT_UPDATE")
                .pathMatchers(HttpMethod.DELETE, "/api/v1/content/**").hasAuthority("PERMISSION_CONTENT_DELETE")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            )
            .build();
    }
}
