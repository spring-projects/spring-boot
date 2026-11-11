/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.autoconfigure;

import java.util.Collections;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

/**
 * Configuration classes for Spring Data for Elasticsearch
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @author Stephane Nicoll
 */
abstract class DataElasticsearchConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchCustomConversions elasticsearchCustomConversions() {
			return new ElasticsearchCustomConversions(Collections.emptyList());
		}

		@Bean
		@ConditionalOnMissingBean
		SimpleElasticsearchMappingContext elasticsearchMappingContext(ApplicationContext applicationContext,
				ElasticsearchCustomConversions elasticsearchCustomConversions) throws ClassNotFoundException {
			SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
			mappingContext.setInitialEntitySet(new EntityScanner(applicationContext).scan(Document.class));
			mappingContext.setSimpleTypeHolder(elasticsearchCustomConversions.getSimpleTypeHolder());
			return mappingContext;
		}

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchConverter elasticsearchConverter(SimpleElasticsearchMappingContext mappingContext,
				ElasticsearchCustomConversions elasticsearchCustomConversions) {
			MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
			converter.setConversions(elasticsearchCustomConversions);
			return converter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ElasticsearchClient.class)
	static class JavaClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ElasticsearchOperations.class, name = "elasticsearchTemplate")
		@ConditionalOnBean(ElasticsearchClient.class)
		ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient client, ElasticsearchConverter converter) {
			return new ElasticsearchTemplate(client, converter);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ElasticsearchTransport.class)
	@ConditionalOnClass({ ReactiveElasticsearchClient.class, ElasticsearchTransport.class, Mono.class })
	static class ReactiveRestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReactiveElasticsearchClient reactiveElasticsearchClient(ElasticsearchTransport transport) {
			return new ReactiveElasticsearchClient(transport);
		}

		@Bean
		@ConditionalOnMissingBean(value = ReactiveElasticsearchOperations.class, name = "reactiveElasticsearchTemplate")
		ReactiveElasticsearchTemplate reactiveElasticsearchTemplate(ReactiveElasticsearchClient client,
				ElasticsearchConverter converter) {
			return new ReactiveElasticsearchTemplate(client, converter);
		}

	}

}
