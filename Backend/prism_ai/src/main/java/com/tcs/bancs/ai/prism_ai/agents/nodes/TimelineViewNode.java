package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.tcs.bancs.ai.prism_ai.agents.prompts.AdvancedSystemPrompt;
import com.tcs.bancs.ai.prism_ai.config.LlmProviderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TimelineViewNode {

    private final Map<String, ChatClient> chatClients;
    private final LlmProviderProperties llmProviderProperties;

    public Map<String, Object> apply(OverAllState state, String question) {

        List<Message> historyMessage = new ArrayList<>();

        if(ObjectUtils.isEmpty(question)){
            question = state.value("message", "");
        }

        if(state.value("history").isPresent()){
            Object history = state.value("history").get();

            if(history instanceof List){
                historyMessage = (List<Message>) history;
            }

        }

        String logSearchResult = state.value("log-search-result", "");

        String userQuery;

        if(!ObjectUtils.isEmpty(logSearchResult)){

            userQuery = """
                
                This is the log result extracted from elastic search based on user query : 
                
                %s
                
                ===========================================================================
                
                this is the user query : 
                
                %s
                
                """.formatted(logSearchResult, question);

        }
        else{
            userQuery = question;
        }

        String responseFromAi = chatClients.get(llmProviderProperties.heavyweight())
                .prompt()
                .messages(historyMessage)
                .system(AdvancedSystemPrompt.TIMELINE_VIEW_SYSTEM_PROMPT)
                .user(userQuery)
                .call()
                .content();

        if(responseFromAi!=null){
            return Map.of("ai-response", responseFromAi);
        }

        return Map.of();
    }

}
