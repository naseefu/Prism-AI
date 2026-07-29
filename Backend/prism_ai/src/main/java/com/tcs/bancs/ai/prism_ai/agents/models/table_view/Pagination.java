package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Pagination(
        int page,
        @JsonProperty("page_size") int pageSize,
        @JsonProperty("total_records") int totalRecords,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("has_next") boolean hasNext,
        @JsonProperty("has_previous") boolean hasPrevious
) {}
