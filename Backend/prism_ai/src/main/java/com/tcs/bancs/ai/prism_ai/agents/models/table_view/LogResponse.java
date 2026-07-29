package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LogResponse(
        String view,
        @JsonProperty("incident_id") String incidentId,
        @JsonProperty("query_context") QueryContext queryContext,
        Summary summary,
        List<ColumnDefinition> columns,
        List<LogRow> rows,
        Pagination pagination,
        @JsonProperty("applied_filters") List<AppliedFilter> appliedFilters,
        @JsonProperty("available_filters") AvailableFilters availableFilters,
        @JsonProperty("data_quality") DataQuality dataQuality,
        List<String> warnings,
        String message
) {}
