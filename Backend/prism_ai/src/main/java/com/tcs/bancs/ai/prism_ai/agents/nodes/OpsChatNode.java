package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.tcs.bancs.ai.prism_ai.agents.models.AiResponseModel;
import com.tcs.bancs.ai.prism_ai.agents.models.table_view.LogResponse;
import com.tcs.bancs.ai.prism_ai.agents.models.timeline_view.TimelineViewResponseDto;
import com.tcs.bancs.ai.prism_ai.dto.RouterIntents;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.SequenceDiagramResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanTreeResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Ops Chat Node — simplified dispatcher that routes each intent to its
 * corresponding node and collects typed DTO results.
 *
 * <p>The TABLE_VIEW, SPAN_TREE, and SEQUENCE_DIAGRAM nodes now return
 * strictly typed DTOs (no more fragile LLM JSON string parsing).
 * AI_SUMMARY and TIMELINE remain LLM-driven (unchanged).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpsChatNode implements NodeAction {

    private final TableViewNode tableViewNode;
    private final AISummaryNode aiSummaryNode;
    private final TimelineViewNode timelineViewNode;
    private final SpanTreeNode spanTreeNode;
    private final SequenceDiagramNode sequenceDiagramNode;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());
        AiResponseModel aiResponseModel = state.value("ai-response", new AiResponseModel());

        if (aiRouterResponse.getIntents() != null
                && !aiRouterResponse.getIntents().isEmpty()) {

            for (RouterIntents intent : aiRouterResponse.getIntents()) {

                switch (intent.getIntent()) {

                    case "TABLE_VIEW" -> {
                        Map<String, Object> aiResult = tableViewNode.apply(state);
                        if (aiResult != null && aiResult.get("ai-response") instanceof LogResponse logResponse) {
                            aiResponseModel.setTableViewResponse(logResponse);
                        }
                    }

                    case "AI_SUMMARY" -> {
                        // LLM-driven — unchanged
                        Map<String, Object> aiResult = aiSummaryNode.apply(state, intent.getQuestion());
                        if (aiResult != null && aiResult.get("ai-response") != null) {
                            aiResponseModel.setAiSummary(aiResult.get("ai-response").toString());
                        }
                    }

                    case "TIMELINE" -> {
                        // LLM-driven — unchanged
                        Map<String, Object> aiResult = timelineViewNode.apply(state, intent.getQuestion());
                        if (aiResult != null && aiResult.get("ai-response") != null) {
                            aiResponseModel.setTimelineViewResponse(
                                    getTimeLineView(aiResult.get("ai-response").toString()));
                        }
                    }

                    case "SPAN_TREE" -> {
                        Map<String, Object> aiResult = spanTreeNode.apply(state);
                        if (aiResult != null && aiResult.get("ai-response") instanceof SpanTreeResponseDTO dto) {
                            aiResponseModel.setSpanTreeResponse(dto);
                        }
                    }

                    case "SEQUENCE_DIAGRAM" -> {
                        Map<String, Object> aiResult = sequenceDiagramNode.apply(state);
                        if (aiResult != null
                                && aiResult.get("ai-response") instanceof SequenceDiagramResponseDTO dto) {
                            aiResponseModel.setSequenceDiagramResponse(dto);
                        }
                    }
                }
            }
        }

        return Map.of("ai-response", aiResponseModel);
    }

    /**
     * Parse the LLM's timeline JSON response into a typed DTO.
     * Still needed because TimelineViewNode remains LLM-driven.
     */
    public TimelineViewResponseDto getTimeLineView(String aiResponse) {
        TimelineViewResponseDto timelineView = null;

        if (aiResponse != null) {
            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}");

            try {
                aiResponse = aiResponse.substring(start, end + 1);
                timelineView = objectMapper.readValue(aiResponse, TimelineViewResponseDto.class);
            } catch (Exception e) {
                log.warn("Failed to parse timeline LLM response: {}", e.getMessage());
            }
        }

        return timelineView;
    }
}
