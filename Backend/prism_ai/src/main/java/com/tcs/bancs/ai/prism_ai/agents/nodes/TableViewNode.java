package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.tcs.bancs.ai.prism_ai.agents.models.table_view.LogResponse;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.services.LogMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Table View Node — programmatically maps Elasticsearch log hits into a
 * {@link LogResponse} DTO using {@link LogMappingService}. No LLM involvement.
 *
 * <p>Reads pre-fetched log hits from {@code state["filteredLogHits"]} (set by
 * {@link ElasticSearchNode}). Returns a fully typed LogResponse.</p>
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class TableViewNode {

    private final LogMappingService logMappingService;

    /**
     * Build the table view response from pre-fetched log hits in state.
     *
     * @return map containing a {@link LogResponse} under "ai-response"
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {

        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());

        // Step 1: Read pre-fetched log hits from state
        List<Map<String, Object>> logHits = Collections.emptyList();

        if (state.value("filteredLogHits").isPresent()) {
            Object raw = state.value("filteredLogHits").get();
            if (raw instanceof List) {
                logHits = (List<Map<String, Object>>) raw;
            }
        }

        // Step 2: Programmatic mapping — zero LLM
        LogResponse logResponse = logMappingService.mapToLogResponse(
                logHits,
                aiRouterResponse.getEntities()
        );

        return Map.of("ai-response", logResponse);
    }
}
