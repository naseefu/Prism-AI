package com.tcs.bancs.ai.prism_ai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcs.bancs.ai.prism_ai.dto.ChatLLMRequestDTO;
import com.tcs.bancs.ai.prism_ai.dto.LLMResponseDTO;
import com.tcs.bancs.ai.prism_ai.services.PrismAiServices;

@RestController
@RequestMapping("/rest/api/v1/ai")
public class AiChatController {
	
	private final PrismAiServices prismAiServices;
	
	public AiChatController(PrismAiServices prismAiServices) {
		this.prismAiServices = prismAiServices;
	}
	
	@PostMapping("/chat")
	public ResponseEntity<LLMResponseDTO> chatWithLLM(@RequestBody ChatLLMRequestDTO chatLLMRequestDTO){
		return ResponseEntity.status(HttpStatus.OK).body(prismAiServices.chatWithLLM(chatLLMRequestDTO.userQuery()));
	}

}
