package com.tcs.bancs.ai.prism_ai.agents.models.timeline_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimelineEventDto {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private EventType eventType;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("relative_offset_ms")
    private Long relativeOffsetMs;

    @JsonProperty("duration_ms")
    private Long durationMs;

    @JsonProperty("service")
    private String service;

    @JsonProperty("level")
    private String level;

    @JsonProperty("status_classification")
    private String statusClassification;

    @JsonProperty("message")
    private String message;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("spanId")
    private String spanId;

    @JsonProperty("parentSpanId")
    private String parentSpanId;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("error_type")
    private String errorType;

    // JsonNode captures the arbitrary nested JSON payload perfectly
    @JsonProperty("raw_log")
    private JsonNode rawLog;

    public enum EventType {
        START_NODE,
        ERROR_NODE,
        SPAN_NODE,
        POINT_EVENT,
        END_NODE
    }
}