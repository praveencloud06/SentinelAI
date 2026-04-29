package com.sentinel.ai.controller;

import com.sentinel.ai.dto.LogUploadRequest;
import com.sentinel.ai.model.Log;
import com.sentinel.ai.service.LogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);
    private final LogService logService;

    @PostMapping("/upload")
    public ResponseEntity<Log> uploadLog(@RequestBody LogUploadRequest request) {
        logger.info("Received log upload request");
        Log savedLog = logService.ingestLog(request);
        return new ResponseEntity<>(savedLog, HttpStatus.CREATED);
    }
}
