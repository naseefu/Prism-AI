package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Summary(
        @JsonProperty("total_records") int totalRecords,
        @JsonProperty("returned_records") int returnedRecords,
        @JsonProperty("error_count") int errorCount,
        @JsonProperty("warning_count") int warningCount,
        @JsonProperty("services_count") int servicesCount,
        @JsonProperty("time_range") TimeRange timeRange
) {}
