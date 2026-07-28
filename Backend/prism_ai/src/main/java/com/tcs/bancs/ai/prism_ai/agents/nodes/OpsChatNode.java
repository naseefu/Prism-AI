package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpsChatNode implements NodeAction {

    private final TableViewNode tableViewNode;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());

        if(aiRouterResponse.getIntent()!=null && !aiRouterResponse.getIntent().isEmpty()){
            for(String intent:aiRouterResponse.getIntent()){

                switch (intent){
                    case "TABLE_VIEW" -> tableViewNode.apply(state);
                }

            }
        }

        return Map.of();
    }

}
