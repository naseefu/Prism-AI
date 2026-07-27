package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.tcs.bancs.ai.prism_ai.agents.prompts.SystemPrompts;
import com.tcs.bancs.ai.prism_ai.config.LlmProviderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AISummaryNode implements NodeAction {

    private final Map<String, ChatClient> chatClients;
    private final LlmProviderProperties llmProviderProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {

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
                .system(SystemPrompts.AI_SUMMARY_SYSTEM_PROMPT)
                .user(state.value("message",""))
                .call()
                .content();

        if(responseFromAi!=null){
            return Map.of("ai-response", responseFromAi);
        }

        return Map.of();
    }
}
