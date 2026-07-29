package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppliedFilter(
        String field,
        Object value
) {}
