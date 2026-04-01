package dev.armanruhit.nexusvas.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enriches downstream requests with headers extracted from the validated JWT:
 *   X-Tenant-Id, X-User-Id, X-User-Roles, X-User-Permissions
 *
 * Runs after Spring Security JWT validation so the JWT is guaranteed valid here.
 */
@Component
@Slf4j
public class JwtHeaderEnrichmentFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();

                    String tenantId = jwt.getClaimAsString("tenant_id");
                    String userId   = jwt.getSubject();
                    List<String> permissions = jwt.getClaimAsStringList("permissions");
                    String permissionsHeader = permissions != null
                        ? String.join(",", permissions)
                        : "";

                    ServerWebExchange enriched = exchange.mutate()
                        .request(req -> req.headers(headers -> {
                            if (tenantId != null) headers.set("X-Tenant-Id", tenantId);
                            if (userId   != null) headers.set("X-User-Id", userId);
                            headers.set("X-User-Permissions", permissionsHeader);
                        }))
                        .build();

                    return chain.filter(enriched);
                }
                return chain.filter(exchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
