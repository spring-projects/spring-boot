/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.elasticsearch.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.SimpleJsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.json.jsonb.JsonbJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jsonb.autoconfigure.JsonbAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
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
class ElasticsearchClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ElasticsearchClientAutoConfiguration.class));

	@Test
	void withoutRestClientThenAutoConfigurationShouldBackOff() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchTransport.class)
			.doesNotHaveBean(JsonpMapper.class)
			.doesNotHaveBean(ElasticsearchClient.class));
	}

	@Test
	void withRestClientAutoConfigurationShouldDefineClientAndSupportingBeans() {
		this.contextRunner.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
				.hasSingleBean(Rest5ClientTransport.class)
				.hasSingleBean(ElasticsearchClient.class));
	}

	@Test
	void withoutJsonbOrJacksonShouldDefineSimpleMapper() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JsonMapper.class, ObjectMapper.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
				.hasSingleBean(SimpleJsonpMapper.class));
	}

	@Test
	void withJsonbShouldDefineJsonbMapper() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JsonMapper.class, ObjectMapper.class))
			.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
				.hasSingleBean(JsonbJsonpMapper.class));
	}

	@Test
	void withJacksonShouldDefineJacksonMapper() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
				.hasSingleBean(Jackson3JsonpMapper.class));
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	void withoutJackson3ShouldDefineJackson2Mapper() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JsonMapper.class))
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
				.hasSingleBean(JacksonJsonpMapper.class));
	}

	@Test
	void withJacksonAndJsonbShouldDefineJacksonMapper() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class, JacksonAutoConfiguration.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(JsonpMapper.class)
				.hasSingleBean(Jackson3JsonpMapper.class));
	}

	@Test
	void withCustomMapperTransportShouldUseIt() {
		this.contextRunner.withUserConfiguration(JsonpMapperConfiguration.class)
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JsonpMapper.class).hasBean("customJsonpMapper");
				JsonpMapper mapper = context.getBean(JsonpMapper.class);
				assertThat(context.getBean(ElasticsearchTransport.class).jsonpMapper()).isSameAs(mapper);
			});
	}

	@Test
	void withCustomTransportClientShouldUseIt() {
		this.contextRunner.withUserConfiguration(TransportConfiguration.class)
			.withUserConfiguration(RestClientConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ElasticsearchTransport.class).hasBean("customElasticsearchTransport");
				ElasticsearchTransport transport = context.getBean(ElasticsearchTransport.class);
				assertThat(context.getBean(ElasticsearchClient.class)._transport()).isSameAs(transport);
			});
	}

	@Test
	void jacksonJsonpMapperDoesNotUseGlobalObjectMapper() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.withBean(ObjectMapper.class)
			.run((context) -> {
				JsonMapper jsonMapper = context.getBean(JsonMapper.class);
				Jackson3JsonpMapper jacksonJsonpMapper = context.getBean(Jackson3JsonpMapper.class);
				assertThat(jacksonJsonpMapper.objectMapper()).isNotSameAs(jsonMapper);
			});
	}

	@Test
	@Deprecated(since = "4.0.0", forRemoval = true)
	void jackson2JsonpMapperDoesNotUseGlobalObjectMapper() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JsonMapper.class))
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withUserConfiguration(RestClientConfiguration.class)
			.withBean(ObjectMapper.class)
			.run((context) -> {
				ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
				JacksonJsonpMapper jacksonJsonpMapper = context.getBean(JacksonJsonpMapper.class);
				assertThat(jacksonJsonpMapper.objectMapper()).isNotSameAs(objectMapper);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientConfiguration {

		@Bean
		Rest5Client restClient() {
			return mock(Rest5Client.class);
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
		ElasticsearchTransport customElasticsearchTransport(JsonpMapper mapper) {
			return mock(ElasticsearchTransport.class);
		}

	}

}
