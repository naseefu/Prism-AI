package com.tcs.bancs.ai.prism_ai.agents.models.timeline_view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryContextDto {
    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("transactionId")
    private String transactionId;
}