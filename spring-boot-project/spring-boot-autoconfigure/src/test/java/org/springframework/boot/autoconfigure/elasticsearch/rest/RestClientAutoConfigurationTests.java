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

package org.springframework.boot.autoconfigure.elasticsearch.rest;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.ElasticsearchContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestClientAutoConfiguration}
 *
 * @author Brian Clozel
 */
public class RestClientAutoConfigurationTests {

	@ClassRule
	public static ElasticsearchContainer elasticsearch = new ElasticsearchContainer();

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

	@Test
	public void configureShouldCreateBothRestClientVariants() {
		this.contextRunner
				.run((context) -> assertThat(context).hasSingleBean(RestClient.class)
						.hasSingleBean(RestHighLevelClient.class));
	}

	@Test
	public void configureWhenCustomClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestClientConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(RestClient.class)
						.hasBean("customRestClient"));
	}

	@Test
	public void configureWhenBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(BuilderCustomizerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(RestClient.class);
					RestClient restClient = context.getBean(RestClient.class);
					assertThat(restClient)
							.hasFieldOrPropertyWithValue("maxRetryTimeoutMillis", 42L);
				});
	}

	@Test
	public void restClientCanQueryElasticsearchNode() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.rest.uris=http://localhost:"
						+ RestClientAutoConfigurationTests.elasticsearch.getMappedPort())
				.run((context) -> {
					RestHighLevelClient client = context
							.getBean(RestHighLevelClient.class);
					Map<String, String> source = new HashMap<>();
					source.put("a", "alpha");
					source.put("b", "bravo");
					IndexRequest index = new IndexRequest("foo", "bar", "1")
							.source(source);
					client.index(index, RequestOptions.DEFAULT);
					GetRequest getRequest = new GetRequest("foo", "bar", "1");
					assertThat(client.get(getRequest, RequestOptions.DEFAULT).isExists())
							.isTrue();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestClientConfiguration {

		@Bean
		public RestClient customRestClient() {
			return mock(RestClient.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BuilderCustomizerConfiguration {

		@Bean
		public RestClientBuilderCustomizer myCustomizer() {
			return (builder) -> builder.setMaxRetryTimeoutMillis(42);
		}

	}

}
