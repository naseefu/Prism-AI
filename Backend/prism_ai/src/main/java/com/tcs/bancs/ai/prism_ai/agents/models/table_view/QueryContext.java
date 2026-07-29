package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryContext(
        String traceId,
        String uuid,
        String transactionId,
        String service,
        String level,
        @JsonProperty("start_time") String startTime,
        @JsonProperty("end_time") String endTime
) {}