package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpanTreeResponseDTO {

    private String view = "span_tree";

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("span_tree")
    private SpanTreeNodeDTO spanTree;

    @JsonProperty("total_spans")
    private int totalSpans;

    @JsonProperty("error_count")
    private int errorCount;

    @JsonProperty("warning_count")
    private int warningCount;

    @JsonProperty("total_duration_ms")
    private Double totalDurationMs;
}
