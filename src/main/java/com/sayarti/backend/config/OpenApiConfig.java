package com.sayarti.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI sayartiOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Sayarti API")
                                .description("REST API for Sayarti mobile clients")
                                .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addParameters("Accept-Language", new HeaderParameter()
                                .name("Accept-Language")
                                .description("Response language: en or ar (default en); regional variants resolve to their supported base language. Human-readable messages and reference labels are localized; codes remain stable.")
                                .schema(new StringSchema()._default("en").addEnumItem("en").addEnumItem("ar"))))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    org.springdoc.core.customizers.OperationCustomizer acceptLanguageHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name("Accept-Language")
                .required(false)
                .description("Optional response language: en or ar; default en. Regional variants use the supported base language.")
                .schema(new StringSchema()._default("en").addEnumItem("en").addEnumItem("ar")));
    }
}
