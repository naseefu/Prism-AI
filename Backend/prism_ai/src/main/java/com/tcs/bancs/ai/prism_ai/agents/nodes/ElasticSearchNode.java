package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.bancs.ai.prism_ai.dto.RouterIntents;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanRecordDTO;
import com.tcs.bancs.ai.prism_ai.services.ElasticSearchService;
import com.tcs.bancs.ai.prism_ai.services.TraceCorrelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * Dual-state Elasticsearch node — performs two complementary fetches depending
 * on the downstream intents:
 *
 * <ul>
 *   <li><b>Action A</b> (TABLE_VIEW / AI_SUMMARY / TIMELINE):
 *       Filtered log search → {@code state["filteredLogHits"]} (Java objects)
 *       + {@code state["log-search-result"]} (serialized JSON for backward compat)</li>
 *   <li><b>Action B</b> (SPAN_TREE / SEQUENCE_DIAGRAM):
 *       Full trace fetch by traceId → {@code state["unfilteredTraceSpans"]}</li>
 * </ul>
 *
 * <p>Both actions can run in a single pass if the query involves mixed intents
 * (e.g., TABLE_VIEW + SPAN_TREE).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchNode implements NodeAction {

    private final ElasticSearchService elasticSearchService;
    private final TraceCorrelationService traceCorrelationService;
    private final ObjectMapper objectMapper;

    /** Intent names that need filtered log hits. */
    private static final Set<String> FILTERED_LOG_INTENTS = Set.of(
            "TABLE_VIEW", "AI_SUMMARY", "TIMELINE"
    );

    /** Intent names that need unfiltered trace spans. */
    private static final Set<String> TRACE_SPAN_INTENTS = Set.of(
            "SPAN_TREE", "SEQUENCE_DIAGRAM"
    );

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {

        RouterResponseDTO aiRouterResponse = state.value("router-response", new RouterResponseDTO());

        // Determine which fetches are needed based on intents
        boolean needFilteredLogs = false;
        boolean needTraceSpans = false;

        if (aiRouterResponse.getIntents() != null) {
            for (RouterIntents intent : aiRouterResponse.getIntents()) {
                String intentName = intent.getIntent();
                if (FILTERED_LOG_INTENTS.contains(intentName)) needFilteredLogs = true;
                if (TRACE_SPAN_INTENTS.contains(intentName)) needTraceSpans = true;
            }
        }

        // Default: if no specific intents detected, do filtered log search (original behavior)
        if (!needFilteredLogs && !needTraceSpans) {
            needFilteredLogs = true;
        }

        Map<String, Object> result = new HashMap<>();

        // --- Action A: Filtered log search ---
        if (needFilteredLogs) {
            List<Map<String, Object>> logSearchResult =
                    elasticSearchService.extractResults(elasticSearchService.searchLogs(aiRouterResponse));

            if (!ObjectUtils.isEmpty(logSearchResult)) {
                // New typed state key (Java objects)
                result.put("filteredLogHits", logSearchResult);
                // Backward compat: serialized JSON for AI_SUMMARY / TIMELINE nodes
                result.put("log-search-result", objectMapper.writeValueAsString(logSearchResult));
            }
        }

        // --- Action B: Full trace span fetch ---
        if (needTraceSpans) {
            String traceId = extractTraceId(aiRouterResponse);

            if (!ObjectUtils.isEmpty(traceId)) {
                List<SpanRecordDTO> spans = traceCorrelationService.fetchSpansByTraceId(traceId);
                if (!spans.isEmpty()) {
                    result.put("unfilteredTraceSpans", spans);
                }
            } else {
                log.warn("SPAN_TREE/SEQUENCE_DIAGRAM intent detected but no traceId in router entities");
            }
        }

        result.put("log-search-status", "completed");
        return result;
    }

    /**
     * Extract traceId from the router response entities.
     */
    private String extractTraceId(RouterResponseDTO routerResponse) {
        if (routerResponse.getEntities() != null
                && !ObjectUtils.isEmpty(routerResponse.getEntities().getTraceId())) {
            return routerResponse.getEntities().getTraceId();
        }
        return null;
    }
}
