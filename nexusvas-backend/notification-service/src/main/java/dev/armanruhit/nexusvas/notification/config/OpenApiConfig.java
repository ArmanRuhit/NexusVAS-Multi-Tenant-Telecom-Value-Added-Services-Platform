package dev.armanruhit.nexusvas.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NexusVAS - Notification Service API")
                .version("1.0.0")
                .description("SMS, Push, and in-app notification dispatch service. " +
                    "Consumes campaign batches and subscription events from RabbitMQ.")
                .contact(new Contact()
                    .name("MIAKI NexusVAS Team")
                    .email("support@miaki.com")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from Auth Service")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
