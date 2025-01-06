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

package org.springframework.boot.autoconfigure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.SimpleJsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.json.jsonb.JsonbJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import org.elasticsearch.client.RestClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configurations for import into {@link ElasticsearchClientAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ElasticsearchClientConfigurations {

	@Import({ JacksonJsonpMapperConfiguration.class, JsonbJsonpMapperConfiguration.class,
			SimpleJsonpMapperConfiguration.class })
	static class JsonpMapperConfiguration {

	}

	@ConditionalOnMissingBean(JsonpMapper.class)
	@ConditionalOnClass(ObjectMapper.class)
	@Configuration(proxyBeanMethods = false)
	static class JacksonJsonpMapperConfiguration {

		@Bean
		JacksonJsonpMapper jacksonJsonpMapper() {
			return new JacksonJsonpMapper();
		}

	}

	@ConditionalOnMissingBean(JsonpMapper.class)
	@ConditionalOnBean(Jsonb.class)
	@Configuration(proxyBeanMethods = false)
	static class JsonbJsonpMapperConfiguration {

		@Bean
		JsonbJsonpMapper jsonbJsonpMapper(Jsonb jsonb) {
			return new JsonbJsonpMapper(JsonProvider.provider(), jsonb);
		}

	}

	@ConditionalOnMissingBean(JsonpMapper.class)
	@Configuration(proxyBeanMethods = false)
	static class SimpleJsonpMapperConfiguration {

		@Bean
		SimpleJsonpMapper simpleJsonpMapper() {
			return new SimpleJsonpMapper();
		}

	}

	@ConditionalOnMissingBean(ElasticsearchTransport.class)
	static class ElasticsearchTransportConfiguration {

		@Bean
		RestClientTransport restClientTransport(RestClient restClient, JsonpMapper jsonMapper,
				ObjectProvider<RestClientOptions> restClientOptions) {
			return new RestClientTransport(restClient, jsonMapper, restClientOptions.getIfAvailable());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ElasticsearchTransport.class)
	static class ElasticsearchClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
			return new ElasticsearchClient(transport);
		}

	}

}
