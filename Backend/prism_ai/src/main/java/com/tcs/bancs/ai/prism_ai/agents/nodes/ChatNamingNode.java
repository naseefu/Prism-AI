package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.bancs.ai.prism_ai.agents.prompts.SystemPrompts;
import com.tcs.bancs.ai.prism_ai.config.LlmProviderProperties;
import com.tcs.bancs.ai.prism_ai.dto.ChatNamingResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

@Component
@Data
public class ChatNamingNode implements NodeAction {

    private final Map<String, ChatClient> chatClients;
    private final LlmProviderProperties llmProviderProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        String chatName = state.value("chat-name", "");
        String userQuery = state.value("message","");

        if(!ObjectUtils.isEmpty(chatName)){
            return Map.of();
        }

        String chatNameResponse = chatClients.get(llmProviderProperties.lightweight())
                .prompt()
                .system(SystemPrompts.CHAT_SESSION_NAMING_SYSTEM_PROMPT)
                .user(state.value("message",""))
                .call()
                .content();

        return Map.of("chat-name",extractChatNameFromLlmResponse(chatNameResponse, userQuery));

    }

    public String extractChatNameFromLlmResponse(String aiResponse, String userQuery){

        if (aiResponse != null) {
            int startIndex = aiResponse.indexOf("{");
            int lastIndex = aiResponse.lastIndexOf("}");

            try {
                return objectMapper.readValue(aiResponse.substring(startIndex, lastIndex+1), ChatNamingResponseDTO.class).getChatName();
            }
            catch (Exception _) {
            }
        }

        return userQuery.substring(0,8);
    }

}
