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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.SimpleJsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.json.jsonb.JsonbJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchClientAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class ElasticsearchClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchClientAutoConfiguration.class));

	@Test
	void withoutRestClientThenAutoConfigurationShouldBackOff() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchTransport.class)
				.doesNotHaveBean(JsonpMapper.class).doesNotHaveBean(ElasticsearchClient.class));
	}

	@Test
	void withRestClientAutoConfigurationShouldDefineClientAndSupportingBeans() {
		this.contextRunner.withUserConfiguration(RestClientConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
						.hasSingleBean(RestClientTransport.class).hasSingleBean(ElasticsearchClient.class));
	}

	@Test
	void withoutJsonbOrJacksonShouldDefineSimpleMapper() {
		this.contextRunner.withUserConfiguration(RestClientConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(JsonpMapper.class).hasSingleBean(SimpleJsonpMapper.class));
	}

	@Test
	void withJsonbShouldDefineJsonbMapper() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class))
				.withUserConfiguration(RestClientConfiguration.class).run((context) -> assertThat(context)
						.hasSingleBean(JsonpMapper.class).hasSingleBean(JsonbJsonpMapper.class));
	}

	@Test
	void withJacksonShouldDefineJacksonMapper() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
				.withUserConfiguration(RestClientConfiguration.class).run((context) -> assertThat(context)
						.hasSingleBean(JsonpMapper.class).hasSingleBean(JacksonJsonpMapper.class));
	}

	@Test
	void withJacksonAndJsonbShouldDefineJacksonMapper() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class, JacksonAutoConfiguration.class))
				.withUserConfiguration(RestClientConfiguration.class).run((context) -> assertThat(context)
						.hasSingleBean(JsonpMapper.class).hasSingleBean(JacksonJsonpMapper.class));
	}

	@Test
	void withCustomMapperTransportShouldUseIt() {
		this.contextRunner.withUserConfiguration(JsonpMapperConfiguration.class)
				.withUserConfiguration(RestClientConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(JsonpMapper.class).hasBean("customJsonpMapper");
					JsonpMapper mapper = context.getBean(JsonpMapper.class);
					assertThat(context.getBean(ElasticsearchTransport.class).jsonpMapper()).isSameAs(mapper);
				});
	}

	@Test
	void withCustomTransportClientShouldUseIt() {
		this.contextRunner.withUserConfiguration(TransportConfiguration.class)
				.withUserConfiguration(RestClientConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(ElasticsearchTransport.class)
							.hasBean("customElasticsearchTransport");
					ElasticsearchTransport transport = context.getBean(ElasticsearchTransport.class);
					assertThat(context.getBean(ElasticsearchClient.class)._transport()).isSameAs(transport);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientConfiguration {

		@Bean
		RestClient restClient() {
			return mock(RestClient.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JsonpMapperConfiguration {

		@Bean
		JsonpMapper customJsonpMapper() {
			return mock(JsonpMapper.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TransportConfiguration {

		@Bean
		ElasticsearchTransport customElasticsearchTransport() {
			return mock(ElasticsearchTransport.class);
		}

	}

}
