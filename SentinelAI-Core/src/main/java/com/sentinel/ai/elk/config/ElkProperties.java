package com.sentinel.ai.elk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for the ELK Investigation module.
 * Bound from the {@code sentinelai.elk.*} namespace.
 */
@Data
@ConfigurationProperties(prefix = "sentinelai.elk")
public class ElkProperties {

    /** Toggle the entire ELK Investigation feature without removing the dependency. */
    private boolean enabled = false;

    /** Elasticsearch REST endpoint (e.g. http://localhost:9200). */
    private String url = "http://localhost:9200";

    /** Elasticsearch index that holds application logs. */
    private String index = "logs-application";

    /**
     * Upper bound on log documents fetched per investigation request.
     * Keeps AI prompts within acceptable token limits.
     */
    private int maxLogResults = 50;
}
