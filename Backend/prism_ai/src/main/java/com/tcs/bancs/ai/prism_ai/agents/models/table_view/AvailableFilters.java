package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AvailableFilters(
        List<String> levels,
        List<String> services,
        List<String> environments,
        @JsonProperty("status_values") List<String> statusValues
) {}
