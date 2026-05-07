package com.sentinel.ai.elk.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.sentinel.ai.elk.config.ElkProperties;
import com.sentinel.ai.elk.exception.ElkInvestigationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Thin wrapper around the Elasticsearch Java Client responsible for
 * executing log-fetch queries against the configured index.
 *
 * <p>Only registered when {@code sentinelai.elk.enabled=true} so the
 * application starts cleanly without a running Elasticsearch instance.
 */
@Component
@ConditionalOnProperty(name = "sentinelai.elk.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ElkClient {

    private static final Logger logger = LoggerFactory.getLogger(ElkClient.class);

    /** Individual log lines longer than this are truncated to keep prompts manageable. */
    private static final int MAX_LINE_CHARS = 500;

    private final ElasticsearchClient elasticsearchClient;
    private final ElkProperties elkProperties;

    /**
     * Queries Elasticsearch for logs matching the given service, severity, and
     * time window.  Results are sorted by {@code timestamp} descending and
     * capped at {@link ElkProperties#getMaxLogResults()}.
     *
     * @param service          target service name (exact-match term filter)
     * @param severity         log severity (e.g. {@code ERROR}) – uppercased before querying
     * @param timeframeMinutes look-back window in minutes from now
     * @return list of {@code message} strings extracted from matching ES documents
     * @throws ElkInvestigationException if the ES call fails with an I/O error
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> fetchLogs(String service, String severity, int timeframeMinutes) {
        Instant now  = Instant.now();
        Instant from = now.minus(timeframeMinutes, ChronoUnit.MINUTES);

        logger.info("Querying ES – index={} service={} severity={} from={} to={}",
                elkProperties.getIndex(), service, severity, from, now);

        try {
            SearchResponse<Map> response = elasticsearchClient.search(s -> s
                    .index(elkProperties.getIndex())
                    .size(elkProperties.getMaxLogResults())
                    .query(q -> q.bool(b -> b
                            // Exact-match filters on keyword fields
                            .filter(f -> f.term(t -> t.field("service").value(service)))
                            .filter(f -> f.term(t -> t.field("severity").value(severity.toUpperCase())))
                            // Date-range filter on the timestamp field
                            .filter(f -> f.range(r -> r.date(d -> d
                                    .field("timestamp")
                                    .gte(from.toString())
                                    .lte(now.toString())
                            )))
                    ))
                    // Most-recent logs first so the AI sees the latest events at the top
                    .sort(sort -> sort.field(f -> f
                            .field("timestamp")
                            .order(SortOrder.Desc)
                    )),
                    Map.class
            );

            List<String> messages = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(doc -> {
                        Object msg = ((Map<String, Object>) doc).get("message");
                        return msg != null ? String.valueOf(msg) : null;
                    })
                    .filter(Objects::nonNull)
                    // Truncate excessively long lines so AI prompts stay within token limits
                    .map(line -> line.length() > MAX_LINE_CHARS
                            ? line.substring(0, MAX_LINE_CHARS) + "…"
                            : line)
                    .collect(Collectors.toList());

            logger.info("ES query returned {} matching log entries", messages.size());
            return messages;

        } catch (IOException e) {
            logger.error("Elasticsearch query failed: {}", e.getMessage(), e);
            throw new ElkInvestigationException("Failed to query Elasticsearch: " + e.getMessage(), e);
        }
    }
}
