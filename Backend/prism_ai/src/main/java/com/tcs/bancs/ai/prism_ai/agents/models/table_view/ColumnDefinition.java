package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ColumnDefinition(
        String field,
        String label,
        @JsonProperty("data_type") String dataType,
        boolean sortable,
        boolean filterable
) {}
