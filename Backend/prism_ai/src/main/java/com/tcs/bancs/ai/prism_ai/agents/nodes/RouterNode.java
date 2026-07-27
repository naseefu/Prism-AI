package com.tcs.bancs.ai.prism_ai.agents.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.bancs.ai.prism_ai.agents.prompts.SystemPrompts;
import com.tcs.bancs.ai.prism_ai.config.LlmProviderProperties;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;

@Component
public class RouterNode implements NodeAction {
	
	private final Map<String, ChatClient> chatClients;
	private final LlmProviderProperties llmProviderProperties;
	private final ObjectMapper mapper;
	
	public RouterNode(Map<String, ChatClient> chatClients, LlmProviderProperties llmProviderProperties) {
		this.chatClients = chatClients;
		this.llmProviderProperties = llmProviderProperties;
		this.mapper = new ObjectMapper();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		List<Message> historyMessage = new ArrayList<>();

		if(state.value("history").isPresent()){
			Object history = state.value("history").get();

			if(history instanceof List){
				historyMessage = (List<Message>) history;
			}

		}
		
		String responseFromAi = chatClients.get(llmProviderProperties.heavyweight())
				.prompt()
				.messages(historyMessage)
				.system(SystemPrompts.ROUTER_SYSTEM_PROMPT)
				.user(state.value("message",""))
				.call()
				.content();
		
		
		RouterResponseDTO response = extractRouterResponseFromString(responseFromAi);
		
		return Map.of("router-response", response);
	}
	
	public RouterResponseDTO extractRouterResponseFromString(String rawAiResponse) {
		
		int startIndex = rawAiResponse.indexOf("{");
		int lastIndex = rawAiResponse.lastIndexOf("}");
		
		try {
			return mapper.readValue(rawAiResponse.substring(startIndex, lastIndex+1), RouterResponseDTO.class);
		}
		catch (Exception e) {
			
		}
		return null;
		
	}

}
