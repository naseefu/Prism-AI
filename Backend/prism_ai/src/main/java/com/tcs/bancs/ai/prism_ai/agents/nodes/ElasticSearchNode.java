package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.services.ElasticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ElasticSearchNode implements NodeAction {

    private final ElasticSearchService elasticSearchService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());

        List<Map<String, Object>> logSearchResult = elasticSearchService.extractResults(elasticSearchService.searchLogs(aiRouterResponse));

        if(!ObjectUtils.isEmpty(logSearchResult)){
            return Map.of("log-search-result", objectMapper.writeValueAsString(logSearchResult)
                    , "log-search-status", "completed");
        }

        return Map.of("log-search-status", "completed");
    }
}
