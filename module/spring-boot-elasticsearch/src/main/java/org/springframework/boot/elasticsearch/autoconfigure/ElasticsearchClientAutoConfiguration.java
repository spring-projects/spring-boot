/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.elasticsearch.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientConfigurations.ElasticsearchClientConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientConfigurations.ElasticsearchTransportConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientConfigurations.JsonpMapperConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch's Java client.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = { ElasticsearchRestClientAutoConfiguration.class },
		afterName = { "org.springframework.boot.jsonb.autoconfigure.JsonbAutoConfiguration" })
@ConditionalOnBean(Rest5Client.class)
@ConditionalOnClass(ElasticsearchClient.class)
@Import({ JsonpMapperConfiguration.class, ElasticsearchTransportConfiguration.class,
		ElasticsearchClientConfiguration.class })
public final class ElasticsearchClientAutoConfiguration {

}
