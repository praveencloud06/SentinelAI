package com.sentinelai.knowledge.api.controller;

import com.sentinelai.knowledge.api.dto.SyncResponse;
import com.sentinelai.knowledge.application.WebhookIngestionService;
import com.sentinelai.knowledge.domain.SourceSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {
    private final WebhookIngestionService webhookIngestionService;

    @PostMapping("/{sourceSystem}")
    public SyncResponse receiveWebhook(@PathVariable SourceSystem sourceSystem,
                                       @RequestHeader Map<String, String> headers,
                                       @RequestBody(required = false) Map<String, Object> payload) {
        return webhookIngestionService.ingest(sourceSystem, headers, payload == null ? Map.of() : payload);
    }
}
