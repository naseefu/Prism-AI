package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawLog(
        String timestamp,
        String message,
        String logLevel,
        String serviceName,
        String traceId,
        String spanId,
        String errorType,
        String errorMessage,
        String stackTrace,
        Map<String, String> labels,
        String indexName,
        Double score
) {}
