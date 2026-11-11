/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.elasticsearch.autoconfigure;

import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientConfigurations.RestClientBuilderConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientConfigurations.RestClientConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientConfigurations.RestClientSnifferConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch REST clients.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = SslAutoConfiguration.class)
@ConditionalOnClass(Rest5ClientBuilder.class)
@EnableConfigurationProperties(ElasticsearchProperties.class)
@Import({ RestClientBuilderConfiguration.class, RestClientConfiguration.class, RestClientSnifferConfiguration.class })
public final class ElasticsearchRestClientAutoConfiguration {

}
