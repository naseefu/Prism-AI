package com.tcs.bancs.ai.prism_ai.agents.models.table_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataQuality(
        @JsonProperty("duplicate_records_removed") int duplicateRecordsRemoved,
        @JsonProperty("missing_timestamp_count") int missingTimestampCount,
        @JsonProperty("missing_traceId_count") int missingTraceIdCount,
        @JsonProperty("missing_spanId_count") int missingSpanIdCount,
        @JsonProperty("missing_service_count") int missingServiceCount
) {}
