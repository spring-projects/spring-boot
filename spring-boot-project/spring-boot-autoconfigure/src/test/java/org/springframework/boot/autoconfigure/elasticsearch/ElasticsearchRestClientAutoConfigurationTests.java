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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Evgeniy Cheban
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchRestClientAutoConfigurationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class));

	@Test
	void configureShouldOnlyCreateHighLevelRestClient() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(RestClient.class)
				.hasSingleBean(RestHighLevelClient.class));
	}

	@Test
	void configureWhenCustomRestClientShouldBackOff() {
		this.contextRunner.withBean("customRestClient", RestClient.class, () -> mock(RestClient.class))
				.run((context) -> assertThat(context).doesNotHaveBean(RestHighLevelClient.class)
						.hasSingleBean(RestClient.class).hasBean("customRestClient"));
	}

	@Test
	void configureWhenCustomRestHighLevelClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestHighLevelClientConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(RestHighLevelClient.class));
	}

	@Test
	void configureWhenDefaultRestClientShouldCreateWhenNoUniqueRestHighLevelClient() {
		this.contextRunner.withUserConfiguration(TwoCustomRestHighLevelClientConfiguration.class).run((context) -> {
			Map<String, RestHighLevelClient> restHighLevelClients = context.getBeansOfType(RestHighLevelClient.class);
			assertThat(restHighLevelClients).hasSize(2);
		});
	}

	@Test
	void configureWhenBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(BuilderCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestHighLevelClient.class);
			RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
			RestClient lowLevelClient = restClient.getLowLevelClient();
			assertThat(lowLevelClient).hasFieldOrPropertyWithValue("pathPrefix", "/test");
			assertThat(lowLevelClient).extracting("client.connmgr.pool.maxTotal").isEqualTo(100);
			assertThat(lowLevelClient).extracting("client.defaultConfig.cookieSpec").isEqualTo("rfc6265-lax");
		});
	}

	@Test
	@Deprecated
	void configureWhenDeprecatedBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(DeprecatedBuilderCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestHighLevelClient.class);
			RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
			assertThat(restClient.getLowLevelClient()).hasFieldOrPropertyWithValue("pathPrefix", "/deprecated");
		});
	}

	@Test
	void configureWithNoTimeoutsApplyDefaults() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestHighLevelClient.class);
			RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
			assertTimeouts(restClient, Duration.ofMillis(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS),
					Duration.ofMillis(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS));
		});
	}

	@Test
	void configureWithCustomTimeouts() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.rest.connection-timeout=15s",
				"spring.elasticsearch.rest.read-timeout=1m").run((context) -> {
					assertThat(context).hasSingleBean(RestHighLevelClient.class);
					RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
					assertTimeouts(restClient, Duration.ofSeconds(15), Duration.ofMinutes(1));
				});
	}

	private static void assertTimeouts(RestHighLevelClient restClient, Duration connectTimeout, Duration readTimeout) {
		assertThat(restClient.getLowLevelClient()).extracting("client.defaultConfig.socketTimeout")
				.isEqualTo(Math.toIntExact(readTimeout.toMillis()));
		assertThat(restClient.getLowLevelClient()).extracting("client.defaultConfig.connectTimeout")
				.isEqualTo(Math.toIntExact(connectTimeout.toMillis()));
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

	@Test
	void configureUriWithUsernameOnly() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.rest.uris=http://user@localhost:9200")
				.run((context) -> {
					RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
					assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
							.containsExactly("http://localhost:9200");
					assertThat(client).extracting("client")
							.extracting("credentialsProvider",
									InstanceOfAssertFactories.type(CredentialsProvider.class))
							.satisfies((credentialsProvider) -> {
								Credentials credentials = credentialsProvider
										.getCredentials(new AuthScope("localhost", 9200));
								assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
								assertThat(credentials.getPassword()).isNull();
							});
				});
	}

	@Test
	void configureUriWithUsernameAndEmptyPassword() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.rest.uris=http://user:@localhost:9200")
				.run((context) -> {
					RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
					assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
							.containsExactly("http://localhost:9200");
					assertThat(client).extracting("client")
							.extracting("credentialsProvider",
									InstanceOfAssertFactories.type(CredentialsProvider.class))
							.satisfies((credentialsProvider) -> {
								Credentials credentials = credentialsProvider
										.getCredentials(new AuthScope("localhost", 9200));
								assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
								assertThat(credentials.getPassword()).isEmpty();
							});
				});
	}

	@Test
	void configureUriWithUsernameAndPasswordWhenUsernameAndPasswordPropertiesSet() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.rest.uris=http://user:password@localhost:9200,localhost:9201",
						"spring.elasticsearch.rest.username=admin", "spring.elasticsearch.rest.password=admin")
				.run((context) -> {
					RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
					assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
							.containsExactly("http://localhost:9200", "http://localhost:9201");
					assertThat(client).extracting("client")
							.extracting("credentialsProvider",
									InstanceOfAssertFactories.type(CredentialsProvider.class))
							.satisfies((credentialsProvider) -> {
								Credentials uriCredentials = credentialsProvider
										.getCredentials(new AuthScope("localhost", 9200));
								assertThat(uriCredentials.getUserPrincipal().getName()).isEqualTo("user");
								assertThat(uriCredentials.getPassword()).isEqualTo("password");
								Credentials defaultCredentials = credentialsProvider
										.getCredentials(new AuthScope("localhost", 9201));
								assertThat(defaultCredentials.getUserPrincipal().getName()).isEqualTo("admin");
								assertThat(defaultCredentials.getPassword()).isEqualTo("admin");
							});
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class BuilderCustomizerConfiguration {

		@Bean
		RestClientBuilderCustomizer myCustomizer() {
			return new RestClientBuilderCustomizer() {

				@Override
				public void customize(RestClientBuilder builder) {
					builder.setPathPrefix("/test");
				}

				@Override
				public void customize(HttpAsyncClientBuilder builder) {
					builder.setMaxConnTotal(100);
				}

				@Override
				public void customize(RequestConfig.Builder builder) {
					builder.setCookieSpec("rfc6265-lax");
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Deprecated
	static class DeprecatedBuilderCustomizerConfiguration {

		@Bean
		org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientBuilderCustomizer myCustomizer() {
			return (builder) -> builder.setPathPrefix("/deprecated");
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
