package com.github.myrrhax.diploma_project.config;

import com.fasterxml.jackson.databind.MapperFeature;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@OpenAPIDefinition(
        info = @Info(
                title = "Diploma Project API",
                version = "1.0.0",
                description = "API для работы с пользователями, схемами, версиями и скриптами"
        ),
        servers = {
                @Server(
                        url = "https://myrrermdev.space",
                        description = "Production server"
                ),
                @Server(
                        url = "http://localhost:8000",
                        description = "Localhost server"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT access token",
        scheme = "bearer",
        bearerFormat = "JWT",
        type = SecuritySchemeType.HTTP,
        in = SecuritySchemeIn.HEADER
)
public class ApplicationConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.featuresToEnable(MapperFeature.DEFAULT_VIEW_INCLUSION);
    }
}
