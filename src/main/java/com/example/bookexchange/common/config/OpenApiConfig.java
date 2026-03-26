package com.example.bookexchange.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addTagsItem(new Tag().name("Authentication").description("Operations with authentication"))
                .addTagsItem(new Tag().name("Users").description("Operations with users"))
                .addTagsItem(new Tag().name("Books").description("Operations with books"))
                .addTagsItem(new Tag().name("Reports").description("Operations with reports"))
                .addTagsItem(new Tag().name("Exchange requests").description("Operations with exchange requests"))
                .addTagsItem(new Tag().name("Exchange offers").description("Operations with exchange offers"))
                .addTagsItem(new Tag().name("Exchange history").description("Operations with exchange history"))
                .addTagsItem(new Tag().name("Administrator services").description("Operations with all services for administrators"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .info(new Info()
                        .title("Book Exchange API")
                        .version("1.0"));
    }

    @Bean
    public OpenApiCustomizer successResponseWrapperCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation ->
                            operation.getResponses().forEach((statusCode, apiResponse) -> {
                                if (apiResponse.getContent() == null) {
                                    return;
                                }

                                MediaType mediaType = apiResponse.getContent().get("application/json");
                                if (mediaType == null || mediaType.getSchema() == null) {
                                    return;
                                }

                                if (statusCode.startsWith("2")) {
                                    Schema<?> originalSchema = mediaType.getSchema();

                                    String ref = originalSchema.get$ref();
                                    if (ref != null && !ref.endsWith("/ApiResponse")) {
                                        Schema<?> wrapped = new ObjectSchema()
                                                .addProperty("success", new BooleanSchema()._default(true))
                                                .addProperty("data", new Schema<>().$ref(ref))
                                                .addProperty("message", new StringSchema().nullable(true).example(null))
                                                .addProperty("error", new ObjectSchema().nullable(true).example(null));

                                        mediaType.setSchema(wrapped);
                                    }
                                }
                            })
                    )
            );
        };
    }

    @Bean
    public OpenApiCustomizer errorResponseWrapperCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation ->
                            operation.getResponses().forEach((statusCode, apiResponse) -> {
                                if (statusCode.startsWith("2")) {
                                    return;
                                }

                                if (apiResponse.getContent() == null) {
                                    return;
                                }

                                MediaType mediaType = apiResponse.getContent().get("application/json");

                                if (mediaType == null) {
                                    return;
                                }

                                Schema<?> wrapped = new ObjectSchema()
                                        .addProperty("success", new BooleanSchema()._default(false))
                                        .addProperty("data", new Schema<>().nullable(true))
                                        .addProperty("message", new Schema<>().nullable(true))
                                        .addProperty("error", new Schema<>().$ref("#/components/schemas/ApiError"));

                                mediaType.setSchema(wrapped);
                            })
                    )
            );
        };
    }
}
