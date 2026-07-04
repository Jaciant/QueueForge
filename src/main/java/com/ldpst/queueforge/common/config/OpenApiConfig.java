package com.ldpst.queueforge.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI queueForgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QueueForge API")
                        .version("v1.0.0")
                        .description("Core API for QueueForge electronic queue management MVP")
                        .contact(new Contact()
                                .name("QueueForge")));
    }

    @Bean
    public GroupedOpenApi queueForgeV1Api() {
        return GroupedOpenApi.builder()
                .group("queueforge-v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}
