/*
 * Copyright 2012-2020 the original author or authors.
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchRestClientAutoConfigurationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class));

	@Test
	void configureShouldCreateBothRestClientVariants() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class).hasSingleBean(RestHighLevelClient.class);
			assertThat(context.getBean(RestClient.class))
					.isSameAs(context.getBean(RestHighLevelClient.class).getLowLevelClient());
		});
	}

	@Test
	void configureWhenCustomClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestClientConfiguration.class)
				.run((context) -> assertThat(context).getBeanNames(RestClient.class).containsOnly("customRestClient"));
	}

	@Test
	void configureWhenCustomRestHighLevelClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestHighLevelClientConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class).hasSingleBean(RestHighLevelClient.class);
			assertThat(context.getBean(RestClient.class))
					.isSameAs(context.getBean(RestHighLevelClient.class).getLowLevelClient());
		});
	}

	@Test
	void configureWhenDefaultRestClientShouldCreateWhenNoUniqueRestHighLevelClient() {
		this.contextRunner.withUserConfiguration(TwoCustomRestHighLevelClientConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class);
			RestClient restClient = context.getBean(RestClient.class);
			Map<String, RestHighLevelClient> restHighLevelClients = context.getBeansOfType(RestHighLevelClient.class);
			assertThat(restHighLevelClients).hasSize(2);
			for (RestHighLevelClient restHighLevelClient : restHighLevelClients.values()) {
				assertThat(restHighLevelClient.getLowLevelClient()).isNotSameAs(restClient);
			}
		});
	}

	@Test
	void configureWhenHighLevelClientIsNotAvailableShouldCreateRestClientOnly() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RestHighLevelClient.class))
				.run((context) -> assertThat(context).hasSingleBean(RestClient.class)
						.doesNotHaveBean(RestHighLevelClient.class));
	}

	@Test
	void configureWhenBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(BuilderCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class);
			RestClient restClient = context.getBean(RestClient.class);
			assertThat(restClient).hasFieldOrPropertyWithValue("pathPrefix", "/test");
		});
	}

	@Test
	void configureWithNoTimeoutsApplyDefaults() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class);
			RestClient restClient = context.getBean(RestClient.class);
			assertTimeouts(restClient, Duration.ofMillis(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS),
					Duration.ofMillis(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS));
		});
	}

	@Test
	void configureWithCustomTimeouts() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.rest.connection-timeout=15s",
				"spring.elasticsearch.rest.read-timeout=1m").run((context) -> {
					assertThat(context).hasSingleBean(RestClient.class);
					RestClient restClient = context.getBean(RestClient.class);
					assertTimeouts(restClient, Duration.ofSeconds(15), Duration.ofMinutes(1));
				});
	}

	private static void assertTimeouts(RestClient restClient, Duration connectTimeout, Duration readTimeout) {
		Object client = ReflectionTestUtils.getField(restClient, "client");
		Object config = ReflectionTestUtils.getField(client, "defaultConfig");
		assertThat(config).hasFieldOrPropertyWithValue("socketTimeout", Math.toIntExact(readTimeout.toMillis()));
		assertThat(config).hasFieldOrPropertyWithValue("connectTimeout", Math.toIntExact(connectTimeout.toMillis()));
	}

	@Test
	void restClientCanQueryElasticsearchNode() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.rest.uris=http://" + elasticsearch.getHttpHostAddress())
				.run((context) -> {
					RestHighLevelClient client = context.getBean(RestHighLevelClient.class);
					Map<String, String> source = new HashMap<>();
					source.put("a", "alpha");
					source.put("b", "bravo");
					IndexRequest index = new IndexRequest("test").id("1").source(source);
					client.index(index, RequestOptions.DEFAULT);
					GetRequest getRequest = new GetRequest("test").id("1");
					assertThat(client.get(getRequest, RequestOptions.DEFAULT).isExists()).isTrue();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestClientConfiguration {

		@Bean
		RestClient customRestClient() {
			return mock(RestClient.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BuilderCustomizerConfiguration {

		@Bean
		RestClientBuilderCustomizer myCustomizer() {
			return (builder) -> builder.setPathPrefix("/test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient customRestHighLevelClient(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoCustomRestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient customRestHighLevelClient(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

		@Bean
		RestHighLevelClient customRestHighLevelClient1(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

	}

}
