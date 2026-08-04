package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpanTreeNodeDTO {

    @JsonProperty("span_id")
    private String spanId;

    @JsonProperty("parent_span_id")
    private String parentSpanId;

    private String service;
    private String endpoint;
    private String timestamp;

    @JsonProperty("duration_ms")
    private Double durationMs;

    /**
     * Execution result classification: "success", "warning", "error"
     */
    private String status;

    /**
     * Visual health indicator: "green" (healthy), "yellow" (slow), "red" (error)
     */
    @JsonProperty("health_color")
    private String healthColor;

    private String message;

    @JsonProperty("log_level")
    private String logLevel;

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("error_message")
    private String errorMessage;

    private List<SpanTreeNodeDTO> children = new ArrayList<>();
}
