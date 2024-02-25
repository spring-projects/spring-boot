/*
 * Copyright 2012-2023 the original author or authors.
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

	/**
     * BaseConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		/**
         * Creates and returns an instance of ElasticsearchCustomConversions if no other bean of the same type is present.
         * 
         * @return the ElasticsearchCustomConversions instance
         */
        @Bean
		@ConditionalOnMissingBean
		ElasticsearchCustomConversions elasticsearchCustomConversions() {
			return new ElasticsearchCustomConversions(Collections.emptyList());
		}

		/**
         * Creates a SimpleElasticsearchMappingContext bean if no other bean of the same type is present in the application context.
         * This method scans the application context for classes annotated with @Document and sets them as the initial entity set for the mapping context.
         * It also sets the simple type holder from the provided ElasticsearchCustomConversions bean.
         * 
         * @param applicationContext The application context used for scanning classes.
         * @param elasticsearchCustomConversions The ElasticsearchCustomConversions bean used for setting the simple type holder.
         * @return The created SimpleElasticsearchMappingContext bean.
         * @throws ClassNotFoundException If a class cannot be found during scanning.
         */
        @Bean
		@ConditionalOnMissingBean
		SimpleElasticsearchMappingContext elasticsearchMappingContext(ApplicationContext applicationContext,
				ElasticsearchCustomConversions elasticsearchCustomConversions) throws ClassNotFoundException {
			SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
			mappingContext.setInitialEntitySet(new EntityScanner(applicationContext).scan(Document.class));
			mappingContext.setSimpleTypeHolder(elasticsearchCustomConversions.getSimpleTypeHolder());
			return mappingContext;
		}

		/**
         * Returns an ElasticsearchConverter bean if no other bean of the same type is present.
         * 
         * @param mappingContext The SimpleElasticsearchMappingContext used by the ElasticsearchConverter.
         * @param elasticsearchCustomConversions The ElasticsearchCustomConversions used by the ElasticsearchConverter.
         * @return The ElasticsearchConverter bean.
         */
        @Bean
		@ConditionalOnMissingBean
		ElasticsearchConverter elasticsearchConverter(SimpleElasticsearchMappingContext mappingContext,
				ElasticsearchCustomConversions elasticsearchCustomConversions) {
			MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
			converter.setConversions(elasticsearchCustomConversions);
			return converter;
		}

	}

	/**
     * JavaClientConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ElasticsearchClient.class)
	static class JavaClientConfiguration {

		/**
         * Creates an ElasticsearchTemplate bean if there is no existing bean of type ElasticsearchOperations with the name "elasticsearchTemplate".
         * This method is conditionally executed only if there is a bean of type ElasticsearchClient.
         * 
         * @param client the ElasticsearchClient instance to be used by the ElasticsearchTemplate
         * @param converter the ElasticsearchConverter instance to be used by the ElasticsearchTemplate
         * @return the ElasticsearchTemplate bean
         */
        @Bean
		@ConditionalOnMissingBean(value = ElasticsearchOperations.class, name = "elasticsearchTemplate")
		@ConditionalOnBean(ElasticsearchClient.class)
		ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient client, ElasticsearchConverter converter) {
			return new ElasticsearchTemplate(client, converter);
		}

	}

	/**
     * ReactiveRestClientConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	static class ReactiveRestClientConfiguration {

		/**
         * Creates a new instance of ReactiveElasticsearchTemplate if no other bean of type ReactiveElasticsearchOperations
         * with the name "reactiveElasticsearchTemplate" is present and if a bean of type ReactiveElasticsearchClient is present.
         * 
         * @param client the ReactiveElasticsearchClient to be used by the ReactiveElasticsearchTemplate
         * @param converter the ElasticsearchConverter to be used by the ReactiveElasticsearchTemplate
         * @return a new instance of ReactiveElasticsearchTemplate
         */
        @Bean
		@ConditionalOnMissingBean(value = ReactiveElasticsearchOperations.class, name = "reactiveElasticsearchTemplate")
		@ConditionalOnBean(ReactiveElasticsearchClient.class)
		ReactiveElasticsearchTemplate reactiveElasticsearchTemplate(ReactiveElasticsearchClient client,
				ElasticsearchConverter converter) {
			return new ReactiveElasticsearchTemplate(client, converter);
		}

	}

}
