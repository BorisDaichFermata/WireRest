package com.wirerest.api.openAPI;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class OpenApiConfig {
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .scheme("Bearer")
                .in(SecurityScheme.In.QUERY);
    }

    @Value("${wirerest.version}")
    private String wirerestVersion;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().addSecurityItem(new SecurityRequirement().
                        addList("Token"))
                .components(new Components().addSecuritySchemes
                        ("Token", createAPIKeyScheme()))
                .info(new Info().title("WireRest")
                        .description("WireRest is a powerful, restful stateless API for Wireguard.\n" +
                                "Default token is 'admin'")
                        .version(wirerestVersion).contact(new Contact().name("FokiDoki - GitHub")
                                .url("https://github.com/FokiDoki/WireRest"))
                        .license(new License().name("License")
                                .url("https://github.com/FokiDoki/WireRest/blob/master/LICENSE")));
    }
}
