package com.tcs.bancs.ai.prism_ai.config;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ObjectUtils;

@Configuration
@EnableConfigurationProperties(LlmProviderProperties.class)
public class DynamicLlmAutoConfiguration {

    private final LlmProviderProperties properties;

    public DynamicLlmAutoConfiguration(LlmProviderProperties properties) {
        this.properties = properties;
    }

    /**
     * Helper to safely configure an insecure SSL/TLS trust manager context for OkHttp.
     */
    private void configureInsecureSsl(org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient.Builder builder) {
        try {
            // 1. Extract the TrustManager to a variable
            X509TrustManager trustManager = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };

            TrustManager[] trustAllCerts = new TrustManager[]{ trustManager };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 2. Pass BOTH the SSLSocketFactory AND the X509TrustManager
            builder.sslSocketFactory(sslContext.getSocketFactory());
            builder.trustManager(trustManager);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure unsafe SSL connection engine", e);
        }
    }

    private void configureInsecureSsl(org.springframework.ai.anthropic.http.okhttp.SpringAiAnthropicHttpClient.Builder builder) {
        try {
            // 1. Extract the TrustManager to a variable
            X509TrustManager trustManager = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };

            TrustManager[] trustAllCerts = new TrustManager[]{ trustManager };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 2. Pass BOTH the SSLSocketFactory AND the X509TrustManager
            builder.sslSocketFactory(sslContext.getSocketFactory());
            builder.trustManager(trustManager);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure unsafe SSL connection engine", e);
        }
    }
    /**
     * High-density factory initializing the exact target provider bean context.
     * Uses Spring AI 2.0 Immutable Option Builders.
     */
    @Bean
    @Primary
    public Map<String, ChatClient> chatModel() {
    	
        return switch (properties.provider()) {

            case OPENAI -> {
            	
            	Map<String, ChatClient> chatClients = new HashMap<String, ChatClient>();
            	
            	List<String> models = properties.models() != null ? properties.models() : List.of("gpt-4o");
            	
                for(String model:models) {
                	var options = OpenAiChatOptions.builder()
                            .apiKey(properties.apiKey())
                            .baseUrl(properties.baseUrl())
                            .model(model)
                            .temperature(properties.temperature())
                            ;
                    if(!ObjectUtils.isEmpty(properties.baseUrl())) {
                    	options.baseUrl(properties.baseUrl());
                    }

                    chatClients.put(model, ChatClient.create(OpenAiChatModel.builder()
                            .options(options.build())
                            .httpClientBuilderCustomizer(builder -> {
                                builder.timeout(Duration.ofMinutes(10));
                                if (!properties.verifySsl()) {
                                    configureInsecureSsl(builder);
                                }
                            })
                            .build()));
                }
                yield chatClients;
            }

            case ANTHROPIC -> {
            	
            	Map<String, ChatClient> chatClients = new HashMap<String, ChatClient>();
            	
            	List<String> models = properties.models() != null ? properties.models() : List.of("claude-3-5-sonnet");
            	
                for(String model:models) {
                	var options = AnthropicChatOptions.builder()
                            .model(model)
                            .temperature(properties.temperature())
                            .build();

                    chatClients.put(model, ChatClient.create(AnthropicChatModel.builder()
                            .options(options)
                            .httpClientBuilderCustomizer(builder -> {
                                // Allow up to 10 minutes for large PDF / multi-rule LLM responses
                                builder.timeout(Duration.ofMinutes(10));
                                if (!properties.verifySsl()) {
                                    configureInsecureSsl(builder);
                                }
                            })
                            .build()));
                }
                yield chatClients;
            }

            case GOOGLE -> {
            	
            	Map<String, ChatClient> chatClients = new HashMap<String, ChatClient>();
            	
            	List<String> models = properties.models() != null ? properties.models() : List.of("claude-3-5-sonnet");
            	
            	
            	for(String model:models) {
            		 var options = GoogleGenAiChatOptions.builder()
                             .model(model)
                             .temperature(properties.temperature())
                             .build();

                     chatClients.put(model, ChatClient.create(GoogleGenAiChatModel.builder()
                             .options(options)
                             .build()));
            	}
            	
            	yield chatClients;
            }
        };
    }
}