package com.polsl.engineering.project.rms.general.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Configuration
class SwaggerConfig {

    static {
        Schema<LocalTime> schema = new Schema<>();
        schema.example(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        SpringDocUtils.getConfig().replaceWithSchema(LocalTime.class, schema);
    }

    @Bean
    OpenAPI openAPI() {
        var components = new Components();
        addSecuritySchema(components);

        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant Management System API")
                        .description("API documentation for the Restaurant Management System")
                        .version("1.0.0")
                ).components(components);
    }

    private static void addSecuritySchema(Components components) {
        components.addSecuritySchemes(
                "Bearer auth",
                new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
        );
    }

}
