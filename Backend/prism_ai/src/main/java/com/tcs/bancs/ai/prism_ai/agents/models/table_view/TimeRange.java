package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TimeRange(
        @JsonProperty("first_timestamp") String firstTimestamp,
        @JsonProperty("last_timestamp") String lastTimestamp
) {}
