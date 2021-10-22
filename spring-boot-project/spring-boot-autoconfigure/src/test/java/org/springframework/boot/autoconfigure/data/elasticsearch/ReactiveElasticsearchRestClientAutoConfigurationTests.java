/*
 * Copyright 2012-2021 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ClientConfiguration.ClientConfigurationCallback;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.CodecConfigurer.DefaultCodecConfig;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class ReactiveElasticsearchRestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveElasticsearchRestClientAutoConfiguration.class));

	@Test
	void configureShouldCreateDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ClientConfiguration.class)
					.hasSingleBean(ReactiveElasticsearchClient.class);
			List<InetSocketAddress> endpoints = context.getBean(ClientConfiguration.class).getEndpoints();
			assertThat(endpoints).hasSize(1);
			assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
			assertThat(endpoints.get(0).getPort()).isEqualTo(9200);
		});
	}

	@Test
	void configureWhenCustomClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomClientConfiguration.class).run((context) -> assertThat(context)
				.hasSingleBean(ReactiveElasticsearchClient.class).hasBean("customClient"));
	}

	@Test
	void configureWhenCustomClientConfig() {
		this.contextRunner.withUserConfiguration(CustomClientConfigConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(ReactiveElasticsearchClient.class)
						.hasSingleBean(ClientConfiguration.class).hasBean("customClientConfiguration"));
	}

	@Test
	@Deprecated
	void whenEndpointIsCustomizedThenClientConfigurationHasCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.client.reactive.endpoints=localhost:9876")
				.run((context) -> {
					List<InetSocketAddress> endpoints = context.getBean(ClientConfiguration.class).getEndpoints();
					assertThat(endpoints).hasSize(1);
					assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(0).getPort()).isEqualTo(9876);
				});
	}

	@Test
	@Deprecated
	void whenMultipleEndpointsAreConfiguredThenClientConfigurationHasMultipleEndpoints() {
		this.contextRunner
				.withPropertyValues("spring.data.elasticsearch.client.reactive.endpoints=localhost:9876,localhost:8765")
				.run((context) -> {
					List<InetSocketAddress> endpoints = context.getBean(ClientConfiguration.class).getEndpoints();
					assertThat(endpoints).hasSize(2);
					assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(0).getPort()).isEqualTo(9876);
					assertThat(endpoints.get(1).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(1).getPort()).isEqualTo(8765);
				});
	}

	@Test
	void whenUriIsCustomizedThenClientConfigurationHasCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://localhost:9876").run((context) -> {
			List<InetSocketAddress> endpoints = context.getBean(ClientConfiguration.class).getEndpoints();
			assertThat(endpoints).hasSize(1);
			assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
			assertThat(endpoints.get(0).getPort()).isEqualTo(9876);
		});
	}

	@Test
	void whenUriHasHttpsSchemeThenClientConfigurationUsesSsl() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=https://localhost:9876").run((context) -> {
			ClientConfiguration clientConfiguration = context.getBean(ClientConfiguration.class);
			List<InetSocketAddress> endpoints = clientConfiguration.getEndpoints();
			assertThat(endpoints).hasSize(1);
			assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
			assertThat(endpoints.get(0).getPort()).isEqualTo(9876);
			assertThat(clientConfiguration.useSsl()).isTrue();
		});
	}

	@Test
	void whenMultipleUrisAreConfiguredThenClientConfigurationHasMultipleEndpoints() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://localhost:9876,http://localhost:8765")
				.run((context) -> {
					List<InetSocketAddress> endpoints = context.getBean(ClientConfiguration.class).getEndpoints();
					assertThat(endpoints).hasSize(2);
					assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(0).getPort()).isEqualTo(9876);
					assertThat(endpoints.get(1).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(1).getPort()).isEqualTo(8765);
				});
	}

	@Test
	void whenMultipleUrisHaveHttpsSchemeThenClientConfigurationUsesSsl() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=https://localhost:9876,https://localhost:8765")
				.run((context) -> {
					ClientConfiguration clientConfiguration = context.getBean(ClientConfiguration.class);
					List<InetSocketAddress> endpoints = clientConfiguration.getEndpoints();
					assertThat(endpoints).hasSize(2);
					assertThat(endpoints.get(0).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(0).getPort()).isEqualTo(9876);
					assertThat(endpoints.get(1).getHostString()).isEqualTo("localhost");
					assertThat(endpoints.get(1).getPort()).isEqualTo(8765);
					assertThat(clientConfiguration.useSsl()).isTrue();
				});
	}

	@Test
	void whenMultipleUrisHaveVaryingSchemesThenRunFails() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=https://localhost:9876,http://localhost:8765")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class)
							.hasRootCauseMessage("Configured Elasticsearch URIs have varying schemes");
				});
	}

	@Test
	void whenUriHasUsernameOnlyThenDefaultAuthorizationHeaderHasUsernameAndEmptyPassword() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://user@localhost:9200").run((context) -> {
			ClientConfiguration clientConfiguration = context.getBean(ClientConfiguration.class);
			assertThat(clientConfiguration.getDefaultHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly(
					"Basic " + Base64.getEncoder().encodeToString("user:".getBytes(StandardCharsets.UTF_8)));
		});
	}

	@Test
	void whenUriHasUsernameAndPasswordThenDefaultAuthorizationHeaderHasUsernameAndPassword() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://user:secret@localhost:9200")
				.run((context) -> {
					ClientConfiguration clientConfiguration = context.getBean(ClientConfiguration.class);
					assertThat(clientConfiguration.getDefaultHeaders().get(HttpHeaders.AUTHORIZATION))
							.containsExactly("Basic " + Base64.getEncoder()
									.encodeToString("user:secret".getBytes(StandardCharsets.UTF_8)));
				});
	}

	@Test
	void whenMultipleUrisHaveVaryingUserInfosThenRunFails() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.uris=http://user:secret@localhost:9876,http://localhost:8765")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class)
							.hasRootCauseMessage("Configured Elasticsearch URIs have varying user infos");
				});
	}

	@Test
	void whenUriUserInfoMatchesUsernameAndPasswordPropertiesThenDefaultAuthorizationHeaderIsConfigured() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://user:secret@localhost:9876",
				"spring.elasticsearch.username=user", "spring.elasticsearch.password=secret").run((context) -> {
					ClientConfiguration clientConfiguration = context.getBean(ClientConfiguration.class);
					assertThat(clientConfiguration.getDefaultHeaders().get(HttpHeaders.AUTHORIZATION))
							.containsExactly("Basic " + Base64.getEncoder()
									.encodeToString("user:secret".getBytes(StandardCharsets.UTF_8)));
				});
	}

	@Test
	void whenUriUserInfoAndUsernameAndPasswordPropertiesDoNotMatchThenRunFails() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.uris=http://user:secret@localhost:9876",
						"spring.elasticsearch.username=alice", "spring.elasticsearch.password=confidential")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class)
							.hasRootCauseMessage("Credentials from URI user info do not match those from "
									+ "spring.elasticsearch.username and spring.elasticsearch.password");
				});
	}

	@Test
	@Deprecated
	void whenConfiguredToUseSslThenClientConfigurationUsesSsl() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.client.reactive.use-ssl=true")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).useSsl()).isTrue());
	}

	@Test
	void whenSocketTimeoutIsNotConfiguredThenClientConfigurationUsesDefault() {
		this.contextRunner.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getSocketTimeout())
				.isEqualTo(Duration.ofSeconds(30)));
	}

	@Test
	void whenConnectionTimeoutIsNotConfiguredThenClientConfigurationUsesDefault() {
		this.contextRunner.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getConnectTimeout())
				.isEqualTo(Duration.ofSeconds(1)));
	}

	@ParameterizedPropertyPrefixTest
	void whenSocketTimeoutIsConfiguredThenClientConfigurationHasCustomSocketTimeout(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "socket-timeout=2s")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getSocketTimeout())
						.isEqualTo(Duration.ofSeconds(2)));
	}

	@ParameterizedPropertyPrefixTest
	void whenConnectionTimeoutIsConfiguredThenClientConfigurationHasCustomConnectTimeout(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "connection-timeout=2s")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getConnectTimeout())
						.isEqualTo(Duration.ofSeconds(2)));
	}

	@Test
	void whenPathPrefixIsConfiguredThenClientConfigurationHasPathPrefix() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.path-prefix=/some/prefix")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getPathPrefix())
						.isEqualTo("/some/prefix"));
	}

	@ParameterizedPropertyPrefixTest
	void whenCredentialsAreConfiguredThenClientConfigurationHasDefaultAuthorizationHeader(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "username=alice", prefix + "password=secret")
				.run((context) -> assertThat(
						context.getBean(ClientConfiguration.class).getDefaultHeaders().get(HttpHeaders.AUTHORIZATION))
								.containsExactly("Basic YWxpY2U6c2VjcmV0"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "spring.elasticsearch.webclient.", "spring.data.elasticsearch.client.reactive." })
	void whenMaxInMemorySizeIsConfiguredThenUnderlyingWebClientHasCustomMaxInMemorySize(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "max-in-memory-size=1MB").run((context) -> {
			WebClient client = configureWebClient(context.getBean(ClientConfiguration.class).getClientConfigurers());
			assertThat(client).extracting("exchangeFunction").extracting("strategies").extracting("codecConfigurer")
					.extracting("defaultCodecs").asInstanceOf(InstanceOfAssertFactories.type(DefaultCodecConfig.class))
					.extracting(DefaultCodecConfig::maxInMemorySize).isEqualTo(1024 * 1024);
		});
	}

	private WebClient configureWebClient(List<ClientConfigurationCallback<?>> callbacks) {
		WebClient webClient = WebClient.create();
		for (ClientConfigurationCallback<?> callback : callbacks) {
			webClient = ((ReactiveRestClients.WebClientConfigurationCallback) callback).configure(webClient);
		}
		return webClient;
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClientConfiguration {

		@Bean
		ReactiveElasticsearchClient customClient() {
			return mock(ReactiveElasticsearchClient.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClientConfigConfiguration {

		@Bean
		ClientConfiguration customClientConfiguration() {
			return ClientConfiguration.localhost();
		}

	}

	@ParameterizedTest
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ValueSource(strings = { "spring.data.elasticsearch.client.reactive.", "spring.elasticsearch." })
	static @interface ParameterizedPropertyPrefixTest {

	}

}
