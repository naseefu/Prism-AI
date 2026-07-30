package com.tcs.bancs.ai.prism_ai.agents.models.timeline_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataQualityDto {

    @JsonProperty("missing_timestamp_count")
    private Integer missingTimestampCount;

    @JsonProperty("incomplete_trace_chain")
    private Boolean incompleteTraceChain;

    @JsonProperty("orphaned_spans_count")
    private Integer orphanedSpansCount;
}
