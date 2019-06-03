/*
 * Copyright 2012-2019 the original author or authors.
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

import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

/**
 * Configuration classes for Spring Data for Elasticsearch
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 */
abstract class ElasticsearchDataConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ElasticsearchConverter elasticsearchConverter(
				SimpleElasticsearchMappingContext mappingContext) {
			return new MappingElasticsearchConverter(mappingContext);
		}

		@Bean
		@ConditionalOnMissingBean
		public SimpleElasticsearchMappingContext mappingContext() {
			return new SimpleElasticsearchMappingContext();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestHighLevelClient.class)
	static class RestHighLevelClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ElasticsearchOperations.class,
				name = "elasticsearchTemplate")
		@ConditionalOnBean(RestHighLevelClient.class)
		public ElasticsearchRestTemplate elasticsearchTemplate(RestHighLevelClient client,
				ElasticsearchConverter converter) {
			return new ElasticsearchRestTemplate(client, converter);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Client.class)
	static class TransportClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ElasticsearchOperations.class,
				name = "elasticsearchTemplate")
		@ConditionalOnBean(Client.class)
		public ElasticsearchTemplate elasticsearchTemplate(Client client,
				ElasticsearchConverter converter) {
			try {
				return new ElasticsearchTemplate(client, converter);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
