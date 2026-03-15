package com.shopee.ecommerce.infrastructure.search;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/**
 * Elasticsearch client configuration.
 * Uses Spring Data Elasticsearch auto-configuration with optional
 * authentication for production clusters.
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        var builder = ClientConfiguration.builder()
            .connectedTo(elasticsearchUri.replace("http://", "").replace("https://", ""));

        if (!username.isBlank()) {
            builder.withBasicAuth(username, password);
        }

        return builder
            .withConnectTimeout(java.time.Duration.ofSeconds(5))
            .withSocketTimeout(java.time.Duration.ofSeconds(30))
            .build();
    }
}
