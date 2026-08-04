package com.tcs.bancs.ai.prism_ai.services;

import com.tcs.bancs.ai.prism_ai.agents.models.table_view.*;
import com.tcs.bancs.ai.prism_ai.dto.RouterEntityDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Programmatic mapper: raw Elasticsearch hits → {@link LogResponse}.
 *
 * <p>Replaces the previous LLM-based TABLE_VIEW generation with deterministic,
 * zero-hallucination mapping. Every field is extracted directly from the ES
 * {@code _source} document.</p>
 */
@Service
@Slf4j
public class LogMappingService {

    private static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * Convert raw ES hits into a fully populated {@link LogResponse}.
     *
     * @param hits           list of trimmed ES hit maps (produced by {@link ElasticSearchService#extractResults})
     * @param routerEntities the parsed router entities carrying traceId, service, level, time window, etc.
     * @return a complete LogResponse ready for the frontend
     */
    public LogResponse mapToLogResponse(List<Map<String, Object>> hits, RouterEntityDTO routerEntities) {
        if (hits == null || hits.isEmpty()) {
            return emptyResponse(routerEntities);
        }

        // --- 1. Map each hit to a LogRow ---
        List<LogRow> rows = new ArrayList<>();
        Set<String> distinctServices = new LinkedHashSet<>();
        Set<String> distinctLevels = new LinkedHashSet<>();
        Set<String> distinctEnvironments = new LinkedHashSet<>();

        int errorCount = 0;
        int warningCount = 0;
        int missingTimestamp = 0;
        int missingTraceId = 0;
        int missingSpanId = 0;
        int missingService = 0;

        String firstTimestamp = null;
        String lastTimestamp = null;

        for (int i = 0; i < hits.size(); i++) {
            Map<String, Object> hit = hits.get(i);

            String timestamp = safeString(hit.get("timestamp"));
            String level = safeString(hit.get("logLevel"));
            String service = safeString(hit.get("serviceName"));
            String message = safeString(hit.get("message"));
            String traceId = safeString(hit.get("traceId"));
            String spanId = safeString(hit.get("spanId"));
            String errorType = safeString(hit.get("errorType"));
            String errorMessage = safeString(hit.get("errorMessage"));
            String stackTrace = safeString(hit.get("stackTrace"));
            String indexName = safeString(hit.get("indexName"));
            Double score = hit.get("score") instanceof Number n ? n.doubleValue() : null;

            // Extract labels
            @SuppressWarnings("unchecked")
            Map<String, String> labels = hit.get("labels") instanceof Map
                    ? (Map<String, String>) hit.get("labels")
                    : null;

            // Severity classification & color
            String severityColor = classifySeverityColor(level, errorType);
            String statusClassification = classifyStatus(level, errorType);

            // Display message: truncate for readability
            String displayMessage = message != null && message.length() > 200
                    ? message.substring(0, 197) + "..."
                    : message;

            // Build RawLog
            RawLog rawLog = new RawLog(timestamp, message, level, service, traceId, spanId,
                    errorType, errorMessage, stackTrace, labels, indexName, score);

            LogRow row = new LogRow(
                    "row-" + (i + 1),       // rowId
                    timestamp,
                    level,
                    severityColor,
                    service,
                    message,
                    displayMessage,
                    traceId,
                    spanId,
                    null,                    // parentSpanId (not in trimmed hits)
                    null,                    // uuid
                    null,                    // transactionId
                    null,                    // correlationId
                    null,                    // api
                    null,                    // httpMethod
                    null,                    // status (HTTP)
                    statusClassification,
                    null,                    // latencyMs
                    null,                    // latencyStatus
                    null,                    // environment
                    null,                    // host
                    null,                    // pod
                    errorType,
                    errorMessage,
                    rawLog
            );

            rows.add(row);

            // Track aggregates
            if (!ObjectUtils.isEmpty(service)) distinctServices.add(service);
            if (!ObjectUtils.isEmpty(level)) distinctLevels.add(level);

            if ("ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level)
                    || !ObjectUtils.isEmpty(errorType)) {
                errorCount++;
            } else if ("WARN".equalsIgnoreCase(level) || "WARNING".equalsIgnoreCase(level)) {
                warningCount++;
            }

            // Data quality
            if (ObjectUtils.isEmpty(timestamp)) missingTimestamp++;
            if (ObjectUtils.isEmpty(traceId)) missingTraceId++;
            if (ObjectUtils.isEmpty(spanId)) missingSpanId++;
            if (ObjectUtils.isEmpty(service)) missingService++;

            // Time range
            if (timestamp != null) {
                if (firstTimestamp == null) firstTimestamp = timestamp;
                lastTimestamp = timestamp;
            }
        }

        // --- 2. Build column definitions (only columns that have data) ---
        List<ColumnDefinition> columns = buildColumnDefinitions(rows);

        // --- 3. Summary ---
        Summary summary = new Summary(
                hits.size(),           // totalRecords
                rows.size(),           // returnedRecords
                errorCount,
                warningCount,
                distinctServices.size(),
                new TimeRange(firstTimestamp, lastTimestamp)
        );

        // --- 4. Pagination (single page, all results) ---
        int totalRecords = hits.size();
        int totalPages = (int) Math.ceil((double) totalRecords / DEFAULT_PAGE_SIZE);
        Pagination pagination = new Pagination(
                1, DEFAULT_PAGE_SIZE, totalRecords, totalPages,
                totalPages > 1, false
        );

        // --- 5. Applied filters (from router entities) ---
        List<AppliedFilter> appliedFilters = buildAppliedFilters(routerEntities);

        // --- 6. Available filters ---
        AvailableFilters availableFilters = new AvailableFilters(
                new ArrayList<>(distinctLevels),
                new ArrayList<>(distinctServices),
                new ArrayList<>(distinctEnvironments),
                List.of("success", "warning", "error")
        );

        // --- 7. Data quality ---
        DataQuality dataQuality = new DataQuality(0, missingTimestamp, missingTraceId, missingSpanId, missingService);

        // --- 8. Query context ---
        QueryContext queryContext = buildQueryContext(routerEntities);

        return new LogResponse(
                "table_view",
                null,              // incidentId
                queryContext,
                summary,
                columns,
                rows,
                pagination,
                appliedFilters,
                availableFilters,
                dataQuality,
                Collections.emptyList(),  // warnings
                null                       // message
        );
    }

    // ─────────────────────────── Helpers ───────────────────────────

    private LogResponse emptyResponse(RouterEntityDTO routerEntities) {
        return new LogResponse(
                "table_view", null,
                buildQueryContext(routerEntities),
                new Summary(0, 0, 0, 0, 0, new TimeRange(null, null)),
                Collections.emptyList(), Collections.emptyList(),
                new Pagination(1, DEFAULT_PAGE_SIZE, 0, 0, false, false),
                Collections.emptyList(),
                new AvailableFilters(List.of(), List.of(), List.of(), List.of()),
                new DataQuality(0, 0, 0, 0, 0),
                Collections.emptyList(), "No log entries found matching your query."
        );
    }

    private QueryContext buildQueryContext(RouterEntityDTO entities) {
        if (entities == null) {
            return new QueryContext(null, null, null, null, null, null, null);
        }
        String startTime = null;
        String endTime = null;
        if (entities.getTimeRanges() != null && !entities.getTimeRanges().isEmpty()) {
            startTime = entities.getTimeRanges().get(0).getStart();
            endTime = entities.getTimeRanges().get(0).getEnd();
        }
        return new QueryContext(
                entities.getTraceId(),
                entities.getUuid(),
                null,   // transactionId — not in RouterEntityDTO
                entities.getServiceName(),
                entities.getLogLevel(),
                startTime,
                endTime
        );
    }

    private List<AppliedFilter> buildAppliedFilters(RouterEntityDTO entities) {
        List<AppliedFilter> filters = new ArrayList<>();
        if (entities == null) return filters;

        if (!ObjectUtils.isEmpty(entities.getTraceId()))
            filters.add(new AppliedFilter("traceId", entities.getTraceId()));
        if (!ObjectUtils.isEmpty(entities.getServiceName()))
            filters.add(new AppliedFilter("service", entities.getServiceName()));
        if (!ObjectUtils.isEmpty(entities.getLogLevel()))
            filters.add(new AppliedFilter("level", entities.getLogLevel()));
        if (!ObjectUtils.isEmpty(entities.getSpanId()))
            filters.add(new AppliedFilter("spanId", entities.getSpanId()));
        if (!ObjectUtils.isEmpty(entities.getErrorType()))
            filters.add(new AppliedFilter("errorType", entities.getErrorType()));

        return filters;
    }

    /**
     * Auto-detect which columns have at least one non-null value and build definitions.
     */
    private List<ColumnDefinition> buildColumnDefinitions(List<LogRow> rows) {
        // Start with the standard columns that are almost always present
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition("timestamp", "Timestamp", "datetime", true, false));
        columns.add(new ColumnDefinition("level", "Level", "string", true, true));
        columns.add(new ColumnDefinition("service", "Service", "string", true, true));
        columns.add(new ColumnDefinition("message", "Message", "string", false, false));

        // Conditionally add columns only if data exists
        boolean hasTraceId = rows.stream().anyMatch(r -> !ObjectUtils.isEmpty(r.traceId()));
        boolean hasSpanId = rows.stream().anyMatch(r -> !ObjectUtils.isEmpty(r.spanId()));
        boolean hasErrorType = rows.stream().anyMatch(r -> !ObjectUtils.isEmpty(r.errorType()));
        boolean hasErrorMessage = rows.stream().anyMatch(r -> !ObjectUtils.isEmpty(r.errorMessage()));

        if (hasTraceId) columns.add(new ColumnDefinition("traceId", "Trace ID", "string", false, true));
        if (hasSpanId) columns.add(new ColumnDefinition("spanId", "Span ID", "string", false, true));
        if (hasErrorType) columns.add(new ColumnDefinition("errorType", "Error Type", "string", true, true));
        if (hasErrorMessage) columns.add(new ColumnDefinition("errorMessage", "Error Message", "string", false, false));

        return columns;
    }

    private String classifySeverityColor(String level, String errorType) {
        if ("ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level)
                || !ObjectUtils.isEmpty(errorType)) {
            return "red";
        }
        if ("WARN".equalsIgnoreCase(level) || "WARNING".equalsIgnoreCase(level)) {
            return "yellow";
        }
        return "green";
    }

    private String classifyStatus(String level, String errorType) {
        if ("ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level)
                || !ObjectUtils.isEmpty(errorType)) {
            return "error";
        }
        if ("WARN".equalsIgnoreCase(level) || "WARNING".equalsIgnoreCase(level)) {
            return "warning";
        }
        return "success";
    }

    private String safeString(Object value) {
        if (value == null) return null;
        return value.toString();
    }
}
