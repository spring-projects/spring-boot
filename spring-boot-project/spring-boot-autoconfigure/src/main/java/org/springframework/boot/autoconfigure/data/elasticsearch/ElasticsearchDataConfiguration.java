/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.util.Collections;

import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.web.reactive.function.client.WebClient;

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
abstract class ElasticsearchDataConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchCustomConversions elasticsearchCustomConversions() {
			return new ElasticsearchCustomConversions(Collections.emptyList());
		}

		@Bean
		@ConditionalOnMissingBean
		SimpleElasticsearchMappingContext mappingContext(
				ElasticsearchCustomConversions elasticsearchCustomConversions) {
			SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
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
	@ConditionalOnClass(RestHighLevelClient.class)
	static class RestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ElasticsearchOperations.class, name = "elasticsearchTemplate")
		@ConditionalOnBean(RestHighLevelClient.class)
		ElasticsearchRestTemplate elasticsearchTemplate(RestHighLevelClient client, ElasticsearchConverter converter) {
			return new ElasticsearchRestTemplate(client, converter);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ WebClient.class, ReactiveElasticsearchOperations.class })
	static class ReactiveRestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ReactiveElasticsearchOperations.class, name = "reactiveElasticsearchTemplate")
		@ConditionalOnBean(ReactiveElasticsearchClient.class)
		ReactiveElasticsearchTemplate reactiveElasticsearchTemplate(ReactiveElasticsearchClient client,
				ElasticsearchConverter converter) {
			ReactiveElasticsearchTemplate template = new ReactiveElasticsearchTemplate(client, converter);
			template.setIndicesOptions(IndicesOptions.strictExpandOpenAndForbidClosed());
			template.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
			return template;
		}

	}

}
