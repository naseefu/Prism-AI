package com.tcs.bancs.ai.prism_ai.agents.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import org.springframework.util.ObjectUtils;


public class RouterNodeToNext implements EdgeAction {

	@Override
	public String apply(OverAllState state) {
		
		RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());
		String logSearchResult = state.value("log-search-status","");

		if(aiRouterResponse.isRequiresLogSearch() && ObjectUtils.isEmpty(logSearchResult)){
			return "LOG_SEARCH";
		}

		String intent = aiRouterResponse.getIntents()!=null? (aiRouterResponse.getIntents().getFirst()!=null?
				aiRouterResponse.getIntents().getFirst().getIntent() : "SYSTEM_CHAT") : "SYSTEM_CHAT";

		return switch (intent){
			case "CONVERSATION_CHAT" -> "CONVERSATION_CHAT";
			case "SYSTEM_CHAT" -> "SYSTEM_CHAT";
            default -> "OPS_CHAT";
        };
	}

}
