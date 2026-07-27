package com.tcs.bancs.ai.prism_ai.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.tcs.bancs.ai.prism_ai.dto.ChatLLMRequestDTO;
import com.tcs.bancs.ai.prism_ai.entity.ChatMessage;
import com.tcs.bancs.ai.prism_ai.entity.ChatSessions;
import com.tcs.bancs.ai.prism_ai.entity.UserEntity;
import com.tcs.bancs.ai.prism_ai.repository.ChatMessageRepo;
import com.tcs.bancs.ai.prism_ai.repository.ChatSessionsRepo;
import com.tcs.bancs.ai.prism_ai.repository.UserEntityRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.tcs.bancs.ai.prism_ai.agents.BuildPrismGraphConfig;
import com.tcs.bancs.ai.prism_ai.dto.LLMResponseDTO;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PrismAiServices {

	private final CompiledGraph buildPrismAiGraph;
	private final ChatMessageRepo chatMessageRepo;
	private final ChatSessionsRepo chatSessionsRepo;
	private final UserEntityRepo userEntityRepo;

	public LLMResponseDTO chatWithLLM(ChatLLMRequestDTO chatLLMReq) {
		
		LLMResponseDTO llmResponse = new LLMResponseDTO();

		if(ObjectUtils.isEmpty(chatLLMReq.userId())){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED ,"User is unauthorized");
		}

		UserEntity userEntity = userEntityRepo.findById(chatLLMReq.userId()).get();

		if(ObjectUtils.isEmpty(userEntity)){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED ,"User is unauthorized");
		}

		ChatSessions chatSessions = null;
		List<ChatMessage> chatMessages = null;
		boolean isChatSessionPresent = false;

		if(chatLLMReq.conversationId().isPresent()){

			chatSessions = chatSessionsRepo.findByConversationId(chatLLMReq.conversationId().get());

			if(chatSessions!=null){
				isChatSessionPresent = true;
				chatMessages = chatMessageRepo.findTop20ByConversationIdOrderByCreatedAtDesc(chatLLMReq.conversationId().get());
			}

		}
				
		try {

			Map<String, Object> llmMetadata = new HashMap<>();
			llmMetadata.put("message", chatLLMReq.userQuery());

			if(isChatSessionPresent){
				llmMetadata.put("chat-name", chatSessions.getSessionName());

				if(chatMessages!=null && !chatMessages.isEmpty()){
					llmMetadata.put("history", loadHistory(chatMessages));
				}

			}

			OverAllState result = buildPrismAiGraph.invoke(llmMetadata).get();
			
			String errorMessage = result.value("error-message", "");
			
			if(!ObjectUtils.isEmpty(errorMessage)) {
				llmResponse.setLlmResponse(errorMessage);
			}
			else {
				llmResponse.setLlmResponse(result.value("ai-response",""));

				if(!isChatSessionPresent){
					chatSessions = new ChatSessions();
					chatSessions.setUser(userEntity);
					chatSessions.setSessionName(result.value("chat-name",""));
					chatSessions.setConversationId(UUID.randomUUID().toString());

					chatSessionsRepo.save(chatSessions);
				}

				chatMessageRepo.save(getChatMessage(chatLLMReq.userQuery(), MessageType.USER, chatSessions.getConversationId()));
				chatMessageRepo.save(getChatMessage(llmResponse.getLlmResponse(), MessageType.ASSISTANT, chatSessions.getConversationId()));

			}

			if(chatSessions!=null && chatSessions.getConversationId()!=null){
				llmResponse.setConversationId(chatSessions.getConversationId());
			}
			
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		
		return llmResponse;
		
	}

	public ChatMessage getChatMessage(String content, MessageType messageType, String convId){
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setRole(messageType);
		chatMessage.setContent(content);
		chatMessage.setConversationId(convId);
		chatMessage.setCreatedAt(Instant.now());

		return chatMessage;
	}

	public List<Message> loadHistory(List<ChatMessage> chatMessages) {
		return 	chatMessages
				.stream()
				.map(m -> m.getRole() == MessageType.USER
						? (Message) new UserMessage(m.getContent())
						: new AssistantMessage(m.getContent()))
				.toList();
	}

}
