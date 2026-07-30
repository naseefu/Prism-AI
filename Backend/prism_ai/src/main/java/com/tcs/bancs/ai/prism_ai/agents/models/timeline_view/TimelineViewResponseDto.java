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
public class TimelineViewResponseDto {

    @JsonProperty("view")
    private String view;

    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("query_context")
    private QueryContextDto queryContext;

    @JsonProperty("timeline_summary")
    private TimelineSummaryDto timelineSummary;

    @JsonProperty("events")
    private List<TimelineEventDto> events;

    // Unmapped events (e.g., missing timestamps) fallback
    @JsonProperty("unmapped_events")
    private List<TimelineEventDto> unmappedEvents;

    @JsonProperty("data_quality")
    private DataQualityDto dataQuality;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("message")
    private String message;
}
