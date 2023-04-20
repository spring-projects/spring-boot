/*
 * Copyright 2012-2022 the original author or authors.
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
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
		SimpleElasticsearchMappingContext mappingContext(ApplicationContext applicationContext,
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
	static class ReactiveRestClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ReactiveElasticsearchOperations.class, name = "reactiveElasticsearchTemplate")
		@ConditionalOnBean(ReactiveElasticsearchClient.class)
		ReactiveElasticsearchTemplate reactiveElasticsearchTemplate(ReactiveElasticsearchClient client,
				ElasticsearchConverter converter) {
			return new ReactiveElasticsearchTemplate(client, converter);
		}

	}

}
