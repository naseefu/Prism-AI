package com.tcs.bancs.ai.prism_ai.dto;

import com.tcs.bancs.ai.prism_ai.agents.models.AiResponseModel;
import lombok.Data;

@Data
public class LLMResponseDTO {
	
	private String errorResponse;
	private String conversationId;
	private AiResponseModel aiResponseModel;

}
