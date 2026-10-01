package br.com.fiap.medistockbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI medistockOpenApi() {
        final String esquemaBearer = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("MediStock API")
                        .description("Backend Smart HAS - Gestao de estoque hospitalar com IA de redistribuicao")
                        .version("v0.1 (Parte 1: Autenticacao)"))
                .addSecurityItem(new SecurityRequirement().addList(esquemaBearer))
                .components(new Components().addSecuritySchemes(esquemaBearer,
                        new SecurityScheme()
                                .name(esquemaBearer)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
