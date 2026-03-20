package com.crew.evalpipeline.evaluation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ToolRegistryService {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private Map<String, ToolDefinition> registry = Collections.emptyMap();

    @PostConstruct
    public void load() {
        ClassPathResource resource = new ClassPathResource("tool-registry.yml");
        try (InputStream inputStream = resource.getInputStream()) {
            ToolRegistryDocument document = yamlMapper.readValue(inputStream, ToolRegistryDocument.class);
            this.registry = document.tools().definitions().stream()
                    .collect(Collectors.toUnmodifiableMap(ToolDefinition::name, Function.identity()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load tool registry", exception);
        }
    }

    public Map<String, ToolDefinition> getRegistry() {
        return registry;
    }

    public record ToolRegistryDocument(ToolList tools) {
    }

    public record ToolList(List<ToolDefinition> definitions) {
    }

    public record ToolDefinition(
            String name,
            String description,
            List<String> requiredParameters,
            Map<String, String> validationPatterns,
            List<String> selectionKeywords
    ) {
    }
}
