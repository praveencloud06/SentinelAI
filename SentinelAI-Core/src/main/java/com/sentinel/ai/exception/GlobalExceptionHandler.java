package com.sentinel.ai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NonTransientAiException.class)
    @ResponseBody
    public ResponseEntity<String> handleNonTransientAiException(NonTransientAiException ex) {
        logger.error("AI provider error: ", ex);
        String hint = "Ollama model not available. Pull it first (Docker): `docker exec -it sentinelai-ollama ollama pull llama3` " +
                "or update `spring.ai.ollama.chat.model` / `spring.ai.ollama.embedding.model` in `src/main/resources/application.yaml`.";
        return new ResponseEntity<>(hint + " Details: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    @ResponseBody
    public ResponseEntity<String> handleHttpClientErrorException(HttpClientErrorException ex) {
        logger.error("HTTP client error: ", ex);
        return new ResponseEntity<>("Upstream HTTP error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<String> handleException(Exception ex) {
        logger.error("Unhandled exception: ", ex);
        return new ResponseEntity<>("Internal server error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
