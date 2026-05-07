package com.sentinel.ai.elk.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ELK module configuration.
 *
 * <p>Spring Boot's Elasticsearch auto-configuration is excluded globally in
 * {@code application.yaml}. This class manually creates the
 * {@link ElasticsearchClient} bean only when {@code sentinelai.elk.enabled=true}.
 *
 * <p>No compatibility headers are set: the ES 8.x Java client's query DSL is
 * wire-compatible with ES 7.17.x for the standard bool/term/date-range queries
 * used by this module. Compatibility headers require xpack security to be enabled
 * on the ES 7.x node, which is not the case for local development.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ElkProperties.class)
public class ElkConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElkConfig.class);

    @Bean
    @ConditionalOnProperty(name = "sentinelai.elk.enabled", havingValue = "true")
    public ElasticsearchClient elasticsearchClient(ElkProperties properties) {
        logger.info("Initialising Elasticsearch client → {}", properties.getUrl());

        RestClient restClient = RestClient
                .builder(HttpHost.create(properties.getUrl()))
                .build();

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
