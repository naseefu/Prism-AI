package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.bancs.ai.prism_ai.agents.models.AiResponseModel;
import com.tcs.bancs.ai.prism_ai.agents.models.table_view.LogResponse;
import com.tcs.bancs.ai.prism_ai.agents.models.timeline_view.TimelineViewResponseDto;
import com.tcs.bancs.ai.prism_ai.dto.RouterIntents;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpsChatNode implements NodeAction {

    private final TableViewNode tableViewNode;
    private final AISummaryNode aiSummaryNode;
    private final TimelineViewNode timelineViewNode;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());
        AiResponseModel aiResponseModel = state.value("ai-response", new AiResponseModel());

        if(aiRouterResponse.getIntents()!=null &&
                !aiRouterResponse.getIntents().isEmpty()){
            Map<String, Object> aiResult = null;

            for(RouterIntents intent:aiRouterResponse.getIntents()){

                switch (intent.getIntent()){
                    case "TABLE_VIEW" -> {
                        aiResult = tableViewNode.apply(state, intent.getQuestion());

                        if(aiResult!=null && aiResult.get("ai-response")!=null){
                            try{
                                LogResponse logResponse = getTableView(aiResult.get("ai-response").toString());
                                aiResponseModel.setTableViewResponse(logResponse);
                            } catch (Exception _) {

                            }
                        }

                    }
                    case "AI_SUMMARY" -> {
                        aiResult = aiSummaryNode.apply(state, intent.getQuestion());
                        aiResponseModel.setAiSummary(aiResult.get("ai-response").toString());
                    }

                    case "TIMELINE" ->{
                        aiResult = timelineViewNode.apply(state, intent.getQuestion());
                        if(aiResult!=null){
                            aiResponseModel.setTimelineViewResponse(getTimeLineView(aiResult.get("ai-response").toString()));
                        }
                    }
                }

            }
        }

        return Map.of("ai-response", aiResponseModel);
    }

    public TimelineViewResponseDto getTimeLineView(String aiResponse){
        TimelineViewResponseDto timelineView = null;

        if(aiResponse!=null){

            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}");

            try{
                aiResponse = aiResponse.substring(start, end+1);
                timelineView = objectMapper.readValue(aiResponse, TimelineViewResponseDto.class);
            } catch (Exception e) {

            }
        }

        return timelineView;
    }

    public LogResponse getTableView(String aiResponse){
        LogResponse tableView = null;

        if(aiResponse!=null){

            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}");

            try{
                aiResponse = aiResponse.substring(start, end+1);
                tableView = objectMapper.readValue(aiResponse, LogResponse.class);
            } catch (Exception e) {

            }
        }

        return tableView;
    }

}
