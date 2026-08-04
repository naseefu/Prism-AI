package com.tcs.bancs.ai.prism_ai.controller;

import com.tcs.bancs.ai.prism_ai.dto.SequenceDiagramResponseDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanRecordDTO;
import com.tcs.bancs.ai.prism_ai.dto.SpanTreeResponseDTO;
import com.tcs.bancs.ai.prism_ai.services.TraceCorrelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Direct REST endpoints for trace visualization — bypasses the AI agent graph
 * for frontends that need to fetch span tree or sequence diagram data directly.
 */
@RestController
@RequestMapping("/rest/api/v1/traces")
@RequiredArgsConstructor
public class TraceController {

    private final TraceCorrelationService traceCorrelationService;

    /**
     * GET /rest/api/v1/traces/{traceId}/tree
     * Returns the hierarchical span tree for a given trace.
     */
    @GetMapping("/{traceId}/tree")
    public ResponseEntity<SpanTreeResponseDTO> getSpanTree(@PathVariable String traceId) {
        List<SpanRecordDTO> spans = traceCorrelationService.fetchSpansByTraceId(traceId);

        if (spans.isEmpty()) {
            SpanTreeResponseDTO empty = new SpanTreeResponseDTO();
            empty.setTraceId(traceId);
            empty.setTotalSpans(0);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(empty);
        }

        SpanTreeResponseDTO tree = traceCorrelationService.buildSpanTree(traceId, spans);
        return ResponseEntity.ok(tree);
    }

    /**
     * GET /rest/api/v1/traces/{traceId}/sequence
     * Returns the sequence diagram for a given trace.
     */
    @GetMapping("/{traceId}/sequence")
    public ResponseEntity<SequenceDiagramResponseDTO> getSequenceDiagram(@PathVariable String traceId) {
        List<SpanRecordDTO> spans = traceCorrelationService.fetchSpansByTraceId(traceId);

        if (spans.isEmpty()) {
            SequenceDiagramResponseDTO empty = new SequenceDiagramResponseDTO();
            empty.setTraceId(traceId);
            empty.setTotalInteractions(0);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(empty);
        }

        SequenceDiagramResponseDTO diagram = traceCorrelationService.buildSequenceDiagram(traceId, spans);
        return ResponseEntity.ok(diagram);
    }
}
