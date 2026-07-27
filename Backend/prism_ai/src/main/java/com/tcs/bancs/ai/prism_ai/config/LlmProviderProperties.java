package com.tcs.bancs.ai.prism_ai.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app.ai")
public record LlmProviderProperties(
        @NotNull Provider provider,
        String baseUrl,
        @NotNull String apiKey,
        List<String> models,
        Double temperature,
        boolean verifySsl,
        String lightweight,
        String mediumweight,
        String heavyweight
) {
    public enum Provider {
        OPENAI, ANTHROPIC, GOOGLE
    }
}
