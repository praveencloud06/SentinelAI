package com.sentinel.ai.controller;

import com.sentinel.ai.dto.LogSearchRequest;
import com.sentinel.ai.dto.LogSearchResult;
import com.sentinel.ai.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogSearchController {
    private static final Logger logger = LoggerFactory.getLogger(LogSearchController.class);
    private final SemanticSearchService semanticSearchService;

    @PostMapping("/search")
    public ResponseEntity<List<LogSearchResult>> searchLogs(@RequestBody LogSearchRequest request) {
        logger.info("Received semantic search request");
        List<LogSearchResult> results = semanticSearchService.search(request.getQuery(), 5);
        return ResponseEntity.ok(results);
    }
}
