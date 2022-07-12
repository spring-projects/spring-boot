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

package org.springframework.boot.autoconfigure.elasticsearch;

import co.elastic.clients.transport.ElasticsearchTransport;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Elasticsearch's
 * reactive client.
 *
 * @author Brian Clozel
 * @since 3.0.0
 */
@AutoConfiguration(after = ElasticsearchClientAutoConfiguration.class)
@ConditionalOnClass({ ReactiveElasticsearchClient.class, ElasticsearchTransport.class, Mono.class })
@EnableConfigurationProperties(ElasticsearchProperties.class)
@Import(ElasticsearchClientConfigurations.ElasticsearchTransportConfiguration.class)
public class ReactiveElasticsearchClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(ElasticsearchTransport.class)
	ReactiveElasticsearchClient reactiveElasticsearchClient(ElasticsearchTransport transport) {
		return new ReactiveElasticsearchClient(transport);
	}

}
