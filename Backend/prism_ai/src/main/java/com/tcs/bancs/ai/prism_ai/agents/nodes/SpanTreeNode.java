package com.tcs.bancs.ai.prism_ai.agents.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanRecordDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanTreeResponseDTO;
import com.tcs.bancs.ai.prism_ai.services.TraceCorrelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Span Tree Node — builds the hierarchical span tree <b>programmatically</b>
 * from pre-fetched trace spans. No LLM involvement.
 *
 * <p>Reads pre-fetched spans from {@code state["unfilteredTraceSpans"]} (set by
 * {@link ElasticSearchNode}). Falls back to a direct ES query if not available.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpanTreeNode {

    private final TraceCorrelationService traceCorrelationService;

    /**
     * Build the span tree from state and return the typed DTO.
     *
     * @return map containing either a {@link SpanTreeResponseDTO} under "ai-response",
     *         or an error message string
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {

        // Extract traceId from router response entities
        String traceId = extractTraceId(state);

        if (ObjectUtils.isEmpty(traceId)) {
            return Map.of("ai-response",
                    "I need a trace ID to build the span tree. Please provide a traceId, " +
                    "e.g., \"Show span tree for traceId abc123\".");
        }

        // Step 1: Try pre-fetched spans from state (set by ElasticSearchNode)
        List<SpanRecordDTO> spans = Collections.emptyList();

        if (state.value("unfilteredTraceSpans").isPresent()) {
            Object raw = state.value("unfilteredTraceSpans").get();
            if (raw instanceof List) {
                spans = (List<SpanRecordDTO>) raw;
            }
        }

        // Step 2: Fallback — fetch directly if not in state
        if (spans.isEmpty()) {
            log.debug("Pre-fetched spans not found in state; fetching directly for traceId={}", traceId);
            spans = traceCorrelationService.fetchSpansByTraceId(traceId);
        }

        if (spans.isEmpty()) {
            return Map.of("ai-response",
                    "No spans found in Elasticsearch for traceId: " + traceId +
                    ". Please verify the trace ID exists and that logs are being ingested.");
        }

        // Step 3: Build the span tree programmatically
        SpanTreeResponseDTO spanTree = traceCorrelationService.buildSpanTree(traceId, spans);

        return Map.of("ai-response", spanTree);
    }

    /**
     * Extract traceId from the router response entities stored in state.
     */
    private String extractTraceId(OverAllState state) {
        RouterResponseDTO routerResponse = state.value("router-response", new RouterResponseDTO());

        if (routerResponse.getEntities() != null
                && !ObjectUtils.isEmpty(routerResponse.getEntities().getTraceId())) {
            return routerResponse.getEntities().getTraceId();
        }

        return null;
    }
}
