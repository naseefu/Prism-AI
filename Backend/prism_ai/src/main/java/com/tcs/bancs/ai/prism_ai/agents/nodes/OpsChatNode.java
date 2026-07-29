package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.bancs.ai.prism_ai.agents.models.AiResponseModel;
import com.tcs.bancs.ai.prism_ai.agents.models.table_view.LogResponse;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpsChatNode implements NodeAction {

    private final TableViewNode tableViewNode;
    private final AISummaryNode aiSummaryNode;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());
        AiResponseModel aiResponseModel = state.value("ai-response", new AiResponseModel());

        if(aiRouterResponse.getIntent()!=null &&
                !aiRouterResponse.getIntent().isEmpty()){
            Map<String, Object> aiResult = null;

            for(String intent:aiRouterResponse.getIntent()){

                switch (intent){
                    case "TABLE_VIEW" -> {
                        aiResult = tableViewNode.apply(state);

                        if(aiResult!=null && aiResult.get("ai-response")!=null){
                            try{
                                LogResponse logResponse = objectMapper.readValue(aiResult.get("ai-response").toString(), LogResponse.class);
                                aiResponseModel.setTableViewResponse(logResponse);
                            } catch (Exception _) {

                            }
                        }

                    }
                    case "AI_SUMMARY" -> {
                        aiResult = aiSummaryNode.apply(state);
                    }
                }

            }
        }

        return Map.of("ai-response", aiResponseModel);
    }

}
