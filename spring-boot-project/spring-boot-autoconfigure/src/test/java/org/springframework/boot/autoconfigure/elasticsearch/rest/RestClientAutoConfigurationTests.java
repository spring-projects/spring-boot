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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchNodeTemplate;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestClientAutoConfiguration}
 *
 * @author Brian Clozel
 */
public class RestClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

	@Test
	public void configureShouldCreateBothRestClientVariants() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class).hasSingleBean(RestHighLevelClient.class);
			assertThat(context.getBean(RestClient.class))
					.isSameAs(context.getBean(RestHighLevelClient.class).getLowLevelClient());
		});
	}

	@Test
	public void configureWhenCustomClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestClientConfiguration.class)
				.run((context) -> assertThat(context).getBeanNames(RestClient.class).containsOnly("customRestClient"));
	}

	@Test
	public void configureWhenCustomRestHighLevelClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestHighLevelClientConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class).hasSingleBean(RestHighLevelClient.class);
			assertThat(context.getBean(RestClient.class))
					.isSameAs(context.getBean(RestHighLevelClient.class).getLowLevelClient());
		});
	}

	@Test
	public void configureWhenDefaultRestClientShouldCreateWhenNoUniqueRestHighLevelClient() {
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
	public void configureWhenHighLevelClientIsNotAvailableShouldCreateRestClientOnly() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RestHighLevelClient.class))
				.run((context) -> assertThat(context).hasSingleBean(RestClient.class)
						.doesNotHaveBean(RestHighLevelClient.class));
	}

	@Test
	public void configureWhenBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(BuilderCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestClient.class);
			RestClient restClient = context.getBean(RestClient.class);
			Field field = ReflectionUtils.findField(RestClient.class, "maxRetryTimeoutMillis");
			ReflectionUtils.makeAccessible(field);
			assertThat(ReflectionUtils.getField(field, restClient)).isEqualTo(42L);
		});
	}

	@Test
	public void restClientCanQueryElasticsearchNode() {
		new ElasticsearchNodeTemplate().doWithNode((node) -> this.contextRunner
				.withPropertyValues("spring.elasticsearch.rest.uris=http://localhost:" + node.getHttpPort())
				.run((context) -> {
					RestHighLevelClient client = context.getBean(RestHighLevelClient.class);
					Map<String, String> source = new HashMap<>();
					source.put("a", "alpha");
					source.put("b", "bravo");
					IndexRequest index = new IndexRequest("foo", "bar", "1").source(source);
					client.index(index, RequestOptions.DEFAULT);
					GetRequest getRequest = new GetRequest("foo", "bar", "1");
					assertThat(client.get(getRequest, RequestOptions.DEFAULT).isExists()).isTrue();
				}));
	}

	@Configuration
	static class CustomRestClientConfiguration {

		@Bean
		public RestClient customRestClient() {
			return mock(RestClient.class);
		}

	}

	@Configuration
	static class BuilderCustomizerConfiguration {

		@Bean
		public RestClientBuilderCustomizer myCustomizer() {
			return (builder) -> builder.setMaxRetryTimeoutMillis(42);
		}

	}

	@Configuration
	static class CustomRestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient customRestHighLevelClient(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

	}

	@Configuration
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
