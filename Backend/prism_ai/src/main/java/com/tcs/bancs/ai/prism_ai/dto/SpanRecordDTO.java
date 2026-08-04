package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpanRecordDTO {

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("span_id")
    private String spanId;

    @JsonProperty("parent_span_id")
    private String parentSpanId;

    private String service;
    private String endpoint;
    private String timestamp;

    @JsonProperty("duration_ms")
    private Double durationMs;

    private String status;
    
    @JsonProperty("log_level")
    private String logLevel;

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("error_message")
    private String errorMessage;

    private String message;
}
