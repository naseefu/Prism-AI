package com.tcs.bancs.ai.prism_ai.agents.models.timeline_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimelineSummaryDto {
    @JsonProperty("total_events")
    private Integer totalEvents;

    @JsonProperty("root_service")
    private String rootService;

    // Keeping as String handles varying ISO-8601 formats smoothly.
    // Can be changed to java.time.OffsetDateTime if your ObjectMapper is configured for it.
    @JsonProperty("start_timestamp")
    private String startTimestamp;

    @JsonProperty("end_timestamp")
    private String endTimestamp;

    @JsonProperty("total_duration_ms")
    private Long totalDurationMs;

    @JsonProperty("error_count")
    private Integer errorCount;

    @JsonProperty("services_involved")
    private List<String> servicesInvolved;
}