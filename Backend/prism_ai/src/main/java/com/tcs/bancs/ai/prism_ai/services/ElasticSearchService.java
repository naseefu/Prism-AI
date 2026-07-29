package com.tcs.bancs.ai.prism_ai.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.tcs.bancs.ai.prism_ai.dto.RouterBusinessEntities;
import com.tcs.bancs.ai.prism_ai.dto.RouterEntityDTO;
import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.RouterTimeRange;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Builds an Elasticsearch query from the intent-classifier output (RCA / AI_SUMMARY intents)
 * and searches across all indices that have data in the trailing 7-day window.
 *
 * Assumes the log4j2 JsonTemplateLayout structure you're shipping:
 *  - @timestamp                (date)
 *  - log.level                 (nested object, e.g. log.level.keyword for exact match)
 *  - trace.id / span.id        (nested objects)
 *  - error.type / error.message / error.stack_trace
 *  - labels.<mdcKey>           (dynamic — comes from your MDC flatten resolver)
 *  - service.name
 *  - message
 */
@Service
@RequiredArgsConstructor
public class ElasticSearchService {

    private final ElasticsearchClient client;

    /**
     * Wildcard index pattern covering your log indices. Adjust to your actual naming
     * convention, e.g. "app-logs-*". A wildcard + @timestamp range filter works
     * regardless of whether you use daily, monthly, or ILM-rollover indices — you
     * do NOT need to enumerate index names by date yourself.
     */
    @Value("${alerthub.elk.index-pattern:*logs*}")
    private String indexPattern;

    /** Hard safety ceiling — never search further back than this, regardless of entity time_range. */
    private static final long MAX_LOOKBACK_DAYS = 7;


    public SearchResponse<Map> searchLogs(RouterResponseDTO payload)  {
        RouterEntityDTO entities = payload.getEntities();

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // --- 1. Time range: explicit entity range wins, clamped to last 7 days ---
        // NOTE: since elasticsearch-java 8.15.0, RangeQuery.Builder no longer has a
        // direct .field()/.gte()/.lte() — range queries are now typed variants
        // (date/number/term/untyped). @timestamp is a date field, so use .date(...),
        // and bounds are date strings, not JsonData.
        RouterTimeRange window = resolveTimeWindow(entities.getTimeRanges());
        bool.filter(f -> f.range(r -> r.date(d -> d
                .field("@timestamp")
                .gte(window.getStart())
                .lte(window.getEnd())
        )));

        // --- 2. Structured filters (only when present) ---
        if (entities.getLogLevel() != null) {
            bool.filter(termFilter("log.level.keyword", entities.getLogLevel()));
        }
        if (entities.getTraceId() != null) {
            bool.filter(termFilter("trace.id", entities.getTraceId()));
        }
        if (entities.getSpanId() != null) {
            bool.filter(termFilter("span.id", entities.getSpanId()));
        }
        if (entities.getServiceName() != null) {
            bool.filter(termFilter("service.name.keyword", entities.getServiceName()));
        }
        if (entities.getErrorType() != null) {
            bool.filter(termFilter("error.type.keyword", entities.getErrorType()));
        }
        if (entities.getUuid() != null) {
            // uuid isn't a fixed field in your template, so treat it as free-text/labels match
            bool.filter(freeTextOrLabelMatch(entities.getUuid(), null));
        }

        // --- 3. Business entities: dynamic field via field_hint, e.g. labels.notificationId ---
        if (entities.getBusinessEntities() != null) {
            for (RouterBusinessEntities be : entities.getBusinessEntities()) {
                String dynamicField = "labels." + be.getFieldHint();
                bool.filter(freeTextOrLabelMatch(be.getId(), dynamicField));
            }
        }

        Query finalQuery = Query.of(q -> q.bool(bool.build()));

        SearchRequest request = SearchRequest.of(s -> s
                .index(indexPattern)
                .query(finalQuery)
                .sort(so -> so.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                .trackScores(true)
                .size(500) // cap results fed into RCA/AI_SUMMARY downstream — tune as needed
        );

        try{
            return client.search(request, Map.class);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Extracts hits from the raw SearchResponse into a simple list of maps —
     * ready to serialize into the RCA/AI_SUMMARY prompt context.
     */
    public List<Map<String, Object>> extractResults(SearchResponse<Map> response) {

        if(response==null){
            return List.of();
        }

        long total = response.hits().total() != null ? response.hits().total().value() : 0;

        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source(); // the actual _source document
            if (source == null) continue;

            // Optional: flatten only the fields you actually need downstream,
            // rather than passing the whole raw document into the LLM prompt.
            Map<String, Object> trimmed = new java.util.LinkedHashMap<>();
            trimmed.put("timestamp", source.get("@timestamp"));
            trimmed.put("message", source.get("message"));
            //trimmed.put("logLevel", extractNested(source, "log", "level"));
            trimmed.put("logLevel", source.get("log.level"));
            trimmed.put("serviceName", source.get("service.name"));
            trimmed.put("traceId", source.get("trace.id"));
            trimmed.put("spanId", source.get("span.id"));
            trimmed.put("errorType", source.get("error.type"));
            trimmed.put("errorMessage", source.get("error.message"));
            trimmed.put("stackTrace", source.get("error.stack_trace"));
            trimmed.put("labels", source.get("labels"));
            trimmed.put("indexName", hit.index()); // which index this hit came from — handy across a wildcard search
            trimmed.put("score", hit.score());

            results.add(trimmed);
        }

        return results;
    }

    /** Safely digs into a nested map field like source.get("log").get("level"). */
    @SuppressWarnings("unchecked")
    private Object extractNested(Map<String, Object> source, String parent, String child) {
        Object nested = source.get(parent);
        if (nested instanceof Map) {
            return ((Map<String, Object>) nested).get(child);
        }
        return null;
    }

    /** Exact term match on a keyword field. */
    private Query termFilter(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }

    /**
     * Matches a business-entity id either against its hinted dynamic field
     * (e.g. labels.notificationId) if the mapping supports it as keyword, OR
     * falls back to a multi-field match across message/labels/error.message.
     * "should" with minimum_should_match(1) so either path can hit — labels.*
     * fields are dynamically mapped and may not always exist as keyword.
     */
    private Query freeTextOrLabelMatch(String value, String dynamicField) {
        BoolQuery.Builder inner = new BoolQuery.Builder().minimumShouldMatch("1");
        if (dynamicField != null) {
            inner.should(s -> s.term(t -> t.field(dynamicField + ".keyword").value(value)));
            inner.should(s -> s.match(m -> m.field(dynamicField).query(value)));
        }
        inner.should(s -> s.multiMatch(mm -> mm
                .query(value)
                .fields("message", "error.message", "error.stack_trace")
        ));
        return Query.of(q -> q.bool(inner.build()));
    }

    private RouterTimeRange resolveTimeWindow(List<RouterTimeRange> timeRanges) {
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(MAX_LOOKBACK_DAYS, ChronoUnit.DAYS);

        if (timeRanges == null || timeRanges.isEmpty()) {
            return new RouterTimeRange(sevenDaysAgo.toString(), now.toString());
        }

        // Use the first explicit range from the intent payload (extend to merge multiple if needed)
        RouterTimeRange tr = timeRanges.get(0);
        Instant start = Instant.parse(tr.getStart() + "Z");
        Instant end = Instant.parse(tr.getEnd() + "Z");

        // Clamp to the 7-day safety window
        if (start.isBefore(sevenDaysAgo)) start = sevenDaysAgo;
        if (end.isAfter(now)) end = now;

        return new RouterTimeRange(start.toString(), end.toString());
    }

}
