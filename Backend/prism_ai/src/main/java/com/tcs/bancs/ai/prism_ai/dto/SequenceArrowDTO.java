package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SequenceArrowDTO {

    private String from;
    private String to;
    private String action;
    private String timestamp;

    @JsonProperty("duration_ms")
    private Double durationMs;

    /**
     * HTTP status or classification: "200", "500", "warning", "error"
     */
    private String status;

    /**
     * Arrow direction type: "request" (solid) or "response" (dashed)
     */
    private String type;
}
