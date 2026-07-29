package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LogRow(
        @JsonProperty("row_id") String rowId,
        String timestamp,
        String level,
        @JsonProperty("severity_color") String severityColor,
        String service,
        String message,
        @JsonProperty("display_message") String displayMessage,
        String traceId,
        String spanId,
        String parentSpanId,
        String uuid,
        String transactionId,
        String correlationId,
        String api,
        @JsonProperty("http_method") String httpMethod,
        Integer status,
        @JsonProperty("status_classification") String statusClassification,
        @JsonProperty("latency_ms") Long latencyMs,
        @JsonProperty("latency_status") String latencyStatus,
        String environment,
        String host,
        String pod,
        @JsonProperty("error_type") String errorType,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("raw_log") RawLog rawLog
) {}
