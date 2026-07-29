package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.tcs.bancs.ai.prism_ai.agents.prompts.SystemPrompts;
import com.tcs.bancs.ai.prism_ai.config.LlmProviderProperties;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class TableViewNode implements NodeAction {

    private final Map<String, ChatClient> chatClients;
    private final LlmProviderProperties llmProviderProperties;

    @Override
    public Map<String, Object> apply(OverAllState state) {

        List<Message> historyMessage = new ArrayList<>();
        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());

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
                
                """.formatted(logSearchResult, state.value("message",""));

        }
        else{
            userQuery = state.value("message", "");
        }

        String responseFromAi = chatClients.get(llmProviderProperties.heavyweight())
                .prompt()
                .messages(historyMessage)
                .system(SystemPrompts.TABLE_VIEW_SYSTEM_PROMPT)
                .user(userQuery)
                .call()
                .content();

        if(responseFromAi!=null){
            return Map.of("ai-response", responseFromAi);
        }

        return Map.of();
    }


}
