package com.tcs.bancs.ai.prism_ai.agents.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;


public class RouterNodeToNext implements EdgeAction {

	@Override
	public String apply(OverAllState state) {
		
		RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());
		String intent = aiRouterResponse.getIntent()!=null? (aiRouterResponse.getIntent().getFirst()!=null?
				aiRouterResponse.getIntent().getFirst() : "SYSTEM_CHAT") : "SYSTEM_CHAT";

		return switch (intent){
			case "CONVERSATION_CHAT" -> "CONVERSATION_CHAT";
			case "SYSTEM_CHAT" -> "SYSTEM_CHAT";
            default -> "OPS_CHAT";
        };
	}

}
