package com.tcs.bancs.ai.prism_ai.services;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.tcs.bancs.ai.prism_ai.dto.LLMResponseDTO;

@Service
public class PrismAiServices {
	
	private final Map<String, ChatClient> chatClients;
	
	public PrismAiServices(Map<String, ChatClient> chatClients) {
		this.chatClients = chatClients;
	}
	
	public LLMResponseDTO chatWithLLM(String userQuery) {
		
		LLMResponseDTO llmResponse = new LLMResponseDTO();
		
		String responseFromAi = chatClients.get("openai/gpt-oss-120b")
				.prompt(userQuery)
				.call()
				.content();
		
		llmResponse.setLlmResponse(responseFromAi);
		
		return llmResponse;
		
	}

}
