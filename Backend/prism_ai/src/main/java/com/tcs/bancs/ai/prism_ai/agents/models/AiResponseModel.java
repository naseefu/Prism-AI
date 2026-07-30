package com.tcs.bancs.ai.prism_ai.agents.models;

import com.tcs.bancs.ai.prism_ai.agents.models.table_view.LogResponse;
import com.tcs.bancs.ai.prism_ai.agents.models.timeline_view.TimelineViewResponseDto;
import lombok.Data;

@Data
public class AiResponseModel {
    private LogResponse tableViewResponse;
    private String aiSummary;
    private TimelineViewResponseDto timelineViewResponse;
}
