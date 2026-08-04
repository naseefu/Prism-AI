package com.tcs.bancs.ai.prism_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SequenceDiagramResponseDTO {

    private String view = "sequence_diagram";

    @JsonProperty("trace_id")
    private String traceId;

    private List<String> participants;
    private List<SequenceArrowDTO> sequence;

    @JsonProperty("total_interactions")
    private int totalInteractions;

    @JsonProperty("error_count")
    private int errorCount;
}
