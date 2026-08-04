package com.tcs.bancs.ai.prism_ai.agents.models;

import com.tcs.bancs.ai.prism_ai.agents.models.table_view.LogResponse;
import com.tcs.bancs.ai.prism_ai.agents.models.timeline_view.TimelineViewResponseDto;
import com.tcs.bancs.ai.prism_ai.dto.SequenceDiagramResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanTreeResponseDTO;
import lombok.Data;

/**
 * Final UI payload containing strictly typed DTOs for each requested view
 * and the LLM's narrative summary.
 */
@Data
public class AiResponseModel {
    private LogResponse tableViewResponse;
    private String aiSummary;
    private TimelineViewResponseDto timelineViewResponse;
    private SpanTreeResponseDTO spanTreeResponse;
    private SequenceDiagramResponseDTO sequenceDiagramResponse;
}
