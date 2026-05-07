package com.sentinel.ai.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PythonAiMlService {
    private static final Logger logger = LoggerFactory.getLogger(PythonAiMlService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${sentinelai.ai-engine.url}")
    private String aiEngineUrl;

    @Value("${sentinelai.ai-engine.provider}")
    private String defaultProvider;

    private final WebClient webClient = WebClient.create();

    public String analyzeLogWithAI(String logText) {
        return analyzeLogWithAI(logText, defaultProvider);
    }

    public String analyzeLogWithAI(String logText, String provider) {
        logger.info("Calling Python AI Engine at {} with provider {}", aiEngineUrl, provider);
        Map<String, Object> requestBody = Map.of("logs", logText);
        String url = aiEngineUrl + "?provider=" + provider;
        try {
            Map<String, Object> resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (resp == null) return "";

            // If the AI-Engine returns structured RCA fields, serialize them into JSON for downstream parsing.
            if (resp.containsKey("issue") || resp.containsKey("rootCause")
                    || resp.containsKey("impactedService") || resp.containsKey("recommendedFix")) {
                try {
                    return OBJECT_MAPPER.writeValueAsString(resp);
                } catch (Exception e) {
                    logger.warn("Failed to serialize AI-Engine response to JSON: {}", e.getMessage());
                }
            }

            Object result = resp.getOrDefault("result", "");
            return result == null ? "" : String.valueOf(result);
        } catch (WebClientResponseException ex) {
            // Surface Python error body to the caller instead of throwing a generic 500.
            String body = ex.getResponseBodyAsString();
            logger.error("AI Engine call failed: status={} body={}", ex.getRawStatusCode(), body);
            return body == null || body.isBlank()
                    ? ("AI Engine HTTP " + ex.getRawStatusCode())
                    : body;
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            logger.error("AI Engine call failed: {}", msg, ex);
            return "AI Engine error: " + msg;
        }
    }
}
