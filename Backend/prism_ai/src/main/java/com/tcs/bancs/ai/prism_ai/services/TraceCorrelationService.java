package com.tcs.bancs.ai.prism_ai.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.tcs.bancs.ai.prism_ai.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Trace Correlation Service — queries Elasticsearch for all spans belonging to a trace
 * and builds both the hierarchical Span Tree and the Sequence Diagram structures.
 *
 * <p>Uses programmatic tree construction (hash-map parent-child linking) rather than
 * LLM-based formatting for structural correctness.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TraceCorrelationService {

    private final ElasticsearchClient client;

    @Value("${alerthub.elk.index-pattern:*logs*}")
    private String indexPattern;

    /** Maximum spans to retrieve per trace. */
    private static final int MAX_SPANS_PER_TRACE = 500;

    /** Latency threshold (ms) above which a span is flagged as "warning" (yellow). */
    private static final double WARNING_LATENCY_THRESHOLD_MS = 500.0;

    // ────────────────────────────────────────────────────────────────────────────
    // 1. Elasticsearch Retrieval
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Fetch all span records for a given traceId from Elasticsearch,
     * sorted by @timestamp ascending.
     */
    public List<SpanRecordDTO> fetchSpansByTraceId(String traceId) {
        if (ObjectUtils.isEmpty(traceId)) {
            return Collections.emptyList();
        }

        SearchRequest request = SearchRequest.of(s -> s
                .index(indexPattern)
                .query(q -> q.term(t -> t.field("trace.id").value(traceId)))
                .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Asc)))
                .size(MAX_SPANS_PER_TRACE)
        );

        try {
            SearchResponse<Map> response = client.search(request, Map.class);
            return extractSpanRecords(response, traceId);
        } catch (IOException e) {
            log.error("Failed to query Elasticsearch for traceId={}: {}", traceId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Convert raw ES hits into a list of SpanRecordDTO objects.
     */
    @SuppressWarnings("unchecked")
    private List<SpanRecordDTO> extractSpanRecords(SearchResponse<Map> response, String traceId) {
        List<SpanRecordDTO> records = new ArrayList<>();
        String rootSpanId = null;

        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source == null) continue;

            SpanRecordDTO span = new SpanRecordDTO();
            span.setTraceId(traceId);
            span.setSpanId(safeString(source.get("span.id")));
            
            // Infer parentSpanId: first span is root, all others are children of root
            if (rootSpanId == null) {
                rootSpanId = span.getSpanId();
                span.setParentSpanId(null);
            } else {
                span.setParentSpanId(rootSpanId);
            }

            span.setService(safeString(source.get("service.name")));
            span.setTimestamp(safeString(source.get("@timestamp")));
            span.setMessage(safeString(source.get("message")));
            span.setLogLevel(safeString(source.get("log.level")));
            span.setErrorType(safeString(source.get("error.type")));
            span.setErrorMessage(safeString(source.get("error.message")));

            // Extract endpoint from labels or message
            span.setEndpoint(safeString(source.get("url.path")));
            if (ObjectUtils.isEmpty(span.getEndpoint())) {
                span.setEndpoint(safeString(source.get("http.target")));
            }

            // Derive status classification
            span.setStatus(classifySpanStatus(span.getLogLevel(), span.getErrorType(), span.getDurationMs()));

            records.add(span);
        }

        return records;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 2. Span Tree Construction
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Build the hierarchical span tree from a flat list of span records.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Create a HashMap keyed by spanId for O(1) lookup.</li>
     *   <li>Identify the root span (parentSpanId is null or empty).</li>
     *   <li>Iterate and attach each child to its parent's children list.</li>
     *   <li>Compute health flags per node.</li>
     * </ol>
     * </p>
     */
    public SpanTreeResponseDTO buildSpanTree(String traceId, List<SpanRecordDTO> spans) {
        SpanTreeResponseDTO response = new SpanTreeResponseDTO();
        response.setTraceId(traceId);
        response.setTotalSpans(spans.size());

        if (spans.isEmpty()) {
            return response;
        }

        // Step 1: Convert to tree nodes and build lookup map
        Map<String, SpanTreeNodeDTO> nodeMap = new LinkedHashMap<>();
        List<SpanTreeNodeDTO> allNodes = new ArrayList<>();

        for (SpanRecordDTO span : spans) {
            SpanTreeNodeDTO node = toTreeNode(span);
            allNodes.add(node);

            // Use spanId as key; skip duplicates (keep first occurrence)
            if (!ObjectUtils.isEmpty(node.getSpanId()) && !nodeMap.containsKey(node.getSpanId())) {
                nodeMap.put(node.getSpanId(), node);
            }
        }

        // Step 2: Identify root and link children
        SpanTreeNodeDTO root = null;
        List<SpanTreeNodeDTO> orphans = new ArrayList<>();

        for (SpanTreeNodeDTO node : allNodes) {
            if (ObjectUtils.isEmpty(node.getParentSpanId())) {
                // Root span
                if (root == null) {
                    root = node;
                } else {
                    // Multiple roots — attach as children of the first root
                    root.getChildren().add(node);
                }
            } else {
                // Child span — find parent
                SpanTreeNodeDTO parent = nodeMap.get(node.getParentSpanId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    orphans.add(node);
                }
            }
        }

        // If no explicit root was found, create a synthetic root
        if (root == null) {
            root = new SpanTreeNodeDTO();
            root.setSpanId("synthetic-root");
            root.setService("unknown-root");
            root.setStatus("success");
            root.setHealthColor("green");
            root.getChildren().addAll(orphans);
        } else {
            // Attach orphans to root
            root.getChildren().addAll(orphans);
        }

        // Step 3: Compute aggregate metrics
        int errorCount = 0;
        int warningCount = 0;
        Double maxDuration = null;

        for (SpanTreeNodeDTO node : allNodes) {
            if ("red".equals(node.getHealthColor())) errorCount++;
            else if ("yellow".equals(node.getHealthColor())) warningCount++;

            if (node.getDurationMs() != null) {
                if (maxDuration == null || node.getDurationMs() > maxDuration) {
                    maxDuration = node.getDurationMs();
                }
            }
        }

        response.setSpanTree(root);
        response.setErrorCount(errorCount);
        response.setWarningCount(warningCount);
        response.setTotalDurationMs(root.getDurationMs() != null ? root.getDurationMs() : maxDuration);

        return response;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 3. Sequence Diagram Construction
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Build the sequence diagram from a flat list of span records.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Extract distinct service lifelines.</li>
     *   <li>Order services by first-appearance timestamp.</li>
     *   <li>Map caller→callee arrows using parentSpanId→spanId relationships.</li>
     *   <li>Generate response arrows (dashed returns).</li>
     *   <li>Sort all arrows by timestamp.</li>
     * </ol>
     * </p>
     */
    public SequenceDiagramResponseDTO buildSequenceDiagram(String traceId, List<SpanRecordDTO> spans) {
        SequenceDiagramResponseDTO response = new SequenceDiagramResponseDTO();
        response.setTraceId(traceId);

        if (spans.isEmpty()) {
            response.setParticipants(Collections.emptyList());
            response.setSequence(Collections.emptyList());
            response.setTotalInteractions(0);
            response.setErrorCount(0);
            return response;
        }

        // Build spanId → SpanRecordDTO lookup
        Map<String, SpanRecordDTO> spanMap = new LinkedHashMap<>();
        for (SpanRecordDTO span : spans) {
            if (!ObjectUtils.isEmpty(span.getSpanId())) {
                spanMap.putIfAbsent(span.getSpanId(), span);
            }
        }

        // Step 1: Extract participants ordered by first appearance
        LinkedHashSet<String> participantSet = new LinkedHashSet<>();
        for (SpanRecordDTO span : spans) {
            String service = span.getService();
            if (!ObjectUtils.isEmpty(service)) {
                participantSet.add(service);
            }
        }
        List<String> participants = new ArrayList<>(participantSet);

        // Step 2: Build interaction arrows
        List<SequenceArrowDTO> arrows = new ArrayList<>();
        int errorCount = 0;

        for (SpanRecordDTO span : spans) {
            String callerService = null;
            String calleeService = span.getService();

            if (ObjectUtils.isEmpty(calleeService)) {
                calleeService = "unknown-service";
            }

            // Determine caller from parent span
            if (!ObjectUtils.isEmpty(span.getParentSpanId())) {
                SpanRecordDTO parentSpan = spanMap.get(span.getParentSpanId());
                if (parentSpan != null && !ObjectUtils.isEmpty(parentSpan.getService())) {
                    callerService = parentSpan.getService();
                }
            }

            // If we have a caller→callee pair (skip if caller == callee within same service)
            if (callerService != null) {
                // Request arrow (solid)
                SequenceArrowDTO requestArrow = new SequenceArrowDTO();
                requestArrow.setFrom(callerService);
                requestArrow.setTo(calleeService);
                requestArrow.setAction(deriveAction(span));
                requestArrow.setTimestamp(span.getTimestamp());
                requestArrow.setDurationMs(span.getDurationMs());
                requestArrow.setStatus(deriveArrowStatus(span));
                requestArrow.setType("request");
                arrows.add(requestArrow);

                if ("error".equals(span.getStatus())) {
                    errorCount++;
                }

                // Response arrow (dashed return)
                SequenceArrowDTO responseArrow = new SequenceArrowDTO();
                responseArrow.setFrom(calleeService);
                responseArrow.setTo(callerService);
                responseArrow.setAction(deriveResponseLabel(span));
                responseArrow.setTimestamp(span.getTimestamp());
                responseArrow.setDurationMs(span.getDurationMs());
                responseArrow.setStatus(deriveArrowStatus(span));
                responseArrow.setType("response");
                arrows.add(responseArrow);
            } else if (ObjectUtils.isEmpty(span.getParentSpanId())) {
                // Root span — user/external → first service
                SequenceArrowDTO rootArrow = new SequenceArrowDTO();
                rootArrow.setFrom("User");
                rootArrow.setTo(calleeService);
                rootArrow.setAction(deriveAction(span));
                rootArrow.setTimestamp(span.getTimestamp());
                rootArrow.setDurationMs(span.getDurationMs());
                rootArrow.setStatus(deriveArrowStatus(span));
                rootArrow.setType("request");
                arrows.add(rootArrow);

                // Add "User" as first participant if root span exists
                if (!participants.contains("User")) {
                    participants.add(0, "User");
                }
            }
        }

        response.setParticipants(participants);
        response.setSequence(arrows);
        response.setTotalInteractions(arrows.size());
        response.setErrorCount(errorCount);

        return response;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────────────────────────────────

    private SpanTreeNodeDTO toTreeNode(SpanRecordDTO span) {
        SpanTreeNodeDTO node = new SpanTreeNodeDTO();
        node.setSpanId(span.getSpanId());
        node.setParentSpanId(span.getParentSpanId());
        node.setService(span.getService());
        node.setEndpoint(span.getEndpoint());
        node.setTimestamp(span.getTimestamp());
        node.setDurationMs(span.getDurationMs());
        node.setStatus(span.getStatus());
        node.setMessage(span.getMessage());
        node.setLogLevel(span.getLogLevel());
        node.setErrorType(span.getErrorType());
        node.setErrorMessage(span.getErrorMessage());
        node.setHealthColor(computeHealthColor(span));
        return node;
    }

    /**
     * Compute visual health color based on span status and latency.
     * - Red: error/fatal log level or error status
     * - Yellow: latency > threshold
     * - Green: healthy
     */
    private String computeHealthColor(SpanRecordDTO span) {
        // Red: errors
        if ("error".equalsIgnoreCase(span.getStatus())) {
            return "red";
        }
        String level = span.getLogLevel();
        if ("ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level)) {
            return "red";
        }
        if (!ObjectUtils.isEmpty(span.getErrorType())) {
            return "red";
        }

        // Yellow: high latency
        if (span.getDurationMs() != null && span.getDurationMs() > WARNING_LATENCY_THRESHOLD_MS) {
            return "yellow";
        }

        // Green: healthy
        return "green";
    }

    /**
     * Classify span status from log level and error indicators.
     */
    private String classifySpanStatus(String logLevel, String errorType, Double durationMs) {
        if ("ERROR".equalsIgnoreCase(logLevel) || "FATAL".equalsIgnoreCase(logLevel)) {
            return "error";
        }
        if (!ObjectUtils.isEmpty(errorType)) {
            return "error";
        }
        if ("WARN".equalsIgnoreCase(logLevel) || "WARNING".equalsIgnoreCase(logLevel)) {
            return "warning";
        }
        if (durationMs != null && durationMs > WARNING_LATENCY_THRESHOLD_MS) {
            return "warning";
        }
        return "success";
    }

    /**
     * Derive a human-readable action label for a sequence arrow.
     */
    private String deriveAction(SpanRecordDTO span) {
        if (!ObjectUtils.isEmpty(span.getEndpoint())) {
            return span.getEndpoint();
        }
        if (!ObjectUtils.isEmpty(span.getMessage())) {
            // Truncate message for readability
            String msg = span.getMessage();
            return msg.length() > 60 ? msg.substring(0, 57) + "..." : msg;
        }
        return "invoke";
    }

    /**
     * Derive arrow status string for sequence diagram.
     */
    private String deriveArrowStatus(SpanRecordDTO span) {
        return span.getStatus() != null ? span.getStatus() : "success";
    }

    /**
     * Derive a label for the response arrow.
     */
    private String deriveResponseLabel(SpanRecordDTO span) {
        if ("error".equals(span.getStatus())) {
            return span.getErrorType() != null ? span.getErrorType() : "error response";
        }
        return span.getDurationMs() != null
                ? String.format("%.0fms", span.getDurationMs())
                : "response";
    }

    /**
     * Safely extract a string from a potentially nested object.
     */
    private String safeString(Object value) {
        if (value == null) return null;
        return value.toString();
    }
}
