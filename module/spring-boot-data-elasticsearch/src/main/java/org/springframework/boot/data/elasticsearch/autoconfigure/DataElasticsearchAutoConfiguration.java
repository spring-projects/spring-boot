/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Elasticsearch
 * support.
 *
 * @author Brian Clozel
 * @author Artur Konczak
 * @author Mohsin Husen
 * @since 4.0.0
 * @see EnableElasticsearchRepositories
 * @see EnableReactiveElasticsearchRepositories
 */
@AutoConfiguration(after = ElasticsearchClientAutoConfiguration.class)
@ConditionalOnClass({ ElasticsearchTemplate.class })
@Import({ DataElasticsearchConfiguration.BaseConfiguration.class,
		DataElasticsearchConfiguration.JavaClientConfiguration.class,
		DataElasticsearchConfiguration.ReactiveRestClientConfiguration.class })
public final class DataElasticsearchAutoConfiguration {

}
