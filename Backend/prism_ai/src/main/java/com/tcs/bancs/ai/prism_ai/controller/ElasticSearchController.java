package com.tcs.bancs.ai.prism_ai.controller;

import com.tcs.bancs.ai.prism_ai.dto.RouterResponseDTO;
import com.tcs.bancs.ai.prism_ai.services.ElasticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/api/v1/elk")
@RequiredArgsConstructor
public class ElasticSearchController {

    private final ElasticSearchService elasticSearchService;

    @PostMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchLogsInsideELK(@RequestBody RouterResponseDTO routerResponseDTO){
        return ResponseEntity.status(HttpStatus.OK).body(elasticSearchService.extractResults(elasticSearchService.searchLogs(routerResponseDTO)));
    }

}
