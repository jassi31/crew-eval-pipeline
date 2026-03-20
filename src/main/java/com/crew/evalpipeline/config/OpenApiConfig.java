package com.crew.evalpipeline.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI evalPipelineOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI Agent Evaluation Pipeline")
                .description("Prototype Spring Boot service for ingesting conversations, evaluating agent quality, and generating improvement suggestions.")
                .version("v1")
                .contact(new Contact().name("Crew Take Home Prototype"))
                .license(new License().name("Internal Assignment Prototype")));
    }
}
