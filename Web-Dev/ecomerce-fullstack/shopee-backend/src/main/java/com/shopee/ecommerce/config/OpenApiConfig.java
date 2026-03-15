package com.shopee.ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 configuration.
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Shopee Ecommerce API",
                version = "1.0.0",
                description = "Full-stack Ecommerce REST API — Spring Boot 3 + JWT + OAuth2",
                contact = @Contact(name = "Dev Team", email = "dev@shopee.com"),
                license = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development"),
                @Server(url = "https://api.shopee.com", description = "Production")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Provide the JWT access token obtained from /api/auth/login"
)
public class OpenApiConfig {
    // Configuration via annotations — no additional beans needed
}
