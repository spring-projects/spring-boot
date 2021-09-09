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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
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
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ClientConfiguration.class)
				.hasSingleBean(ReactiveElasticsearchClient.class));
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
	void whenConfiguredToUseSslThenClientConfigurationUsesSsl() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.client.reactive.use-ssl=true")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).useSsl()).isTrue());
	}

	@Test
	void whenSocketTimeoutIsNotConfiguredThenClientConfigurationUsesDefault() {
		this.contextRunner.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getSocketTimeout())
				.isEqualTo(Duration.ofSeconds(5)));
	}

	@Test
	void whenConnectionTimeoutIsNotConfiguredThenClientConfigurationUsesDefault() {
		this.contextRunner.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getConnectTimeout())
				.isEqualTo(Duration.ofSeconds(10)));
	}

	@Test
	void whenSocketTimeoutIsConfiguredThenClientConfigurationHasCustomSocketTimeout() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.client.reactive.socket-timeout=2s")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getSocketTimeout())
						.isEqualTo(Duration.ofSeconds(2)));
	}

	@Test
	void whenConnectionTimeoutIsConfiguredThenClientConfigurationHasCustomConnectTimeout() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.client.reactive.connection-timeout=2s")
				.run((context) -> assertThat(context.getBean(ClientConfiguration.class).getConnectTimeout())
						.isEqualTo(Duration.ofSeconds(2)));
	}

	@Test
	void whenCredentialsAreConfiguredThenClientConfigurationHasDefaultAuthorizationHeader() {
		this.contextRunner
				.withPropertyValues("spring.data.elasticsearch.client.reactive.username=alice",
						"spring.data.elasticsearch.client.reactive.password=secret")
				.run((context) -> assertThat(
						context.getBean(ClientConfiguration.class).getDefaultHeaders().get(HttpHeaders.AUTHORIZATION))
								.containsExactly("Basic YWxpY2U6c2VjcmV0"));
	}

	@Test
	void whenMaxInMemorySizeIsConfiguredThenUnderlyingWebClientHasCustomMaxInMemorySize() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.client.reactive.max-in-memory-size=1MB")
				.run((context) -> {
					WebClient client = context.getBean(ClientConfiguration.class).getWebClientConfigurer()
							.apply(WebClient.create());
					assertThat(client).extracting("exchangeFunction").extracting("strategies")
							.extracting("codecConfigurer").extracting("defaultCodecs")
							.asInstanceOf(InstanceOfAssertFactories.type(DefaultCodecConfig.class))
							.extracting(DefaultCodecConfig::maxInMemorySize).isEqualTo(1024 * 1024);
				});
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

}
