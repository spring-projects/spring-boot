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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientMetricsConfiguration}
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
public class WebClientMetricsConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class,
					HttpClientMetricsAutoConfiguration.class));

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void webClientCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			validateWebClient(builder, registry);
		});
	}

	@Test
	public void shouldNotOverrideCustomTagsProvider() {
		this.contextRunner.withUserConfiguration(CustomTagsProviderConfig.class)
				.run((context) -> assertThat(context)
						.getBeans(WebClientExchangeTagsProvider.class).hasSize(1)
						.containsKey("customTagsProvider"));
	}

	@Test
	public void afterMaxUrisReachedFurtherUrisAreDenied() {
		this.contextRunner
				.withPropertyValues("management.metrics.web.client.max-uri-tags=2")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.client.requests").meters()).hasSize(2);
					assertThat(this.output.toString()).contains(
							"Reached the maximum number of URI tags for 'http.client.requests'.")
							.contains("Are you using 'uriVariables'?");
				});
	}

	@Test
	public void shouldNotDenyNorLogIfMaxUrisIsNotReached() {
		this.contextRunner
				.withPropertyValues("management.metrics.web.client.max-uri-tags=5")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.client.requests").meters()).hasSize(3);
					assertThat(this.output.toString()).doesNotContain(
							"Reached the maximum number of URI tags for 'http.client.requests'.")
							.doesNotContain("Are you using 'uriVariables'?");
				});
	}

	private MeterRegistry getInitializedMeterRegistry(
			AssertableApplicationContext context) {
		WebClient webClient = mockWebClient(context.getBean(WebClient.Builder.class));
		MeterRegistry registry = context.getBean(MeterRegistry.class);
		for (int i = 0; i < 3; i++) {
			webClient.get().uri("http://example.org/projects/" + i).exchange()
					.block(Duration.ofSeconds(30));
		}
		return registry;
	}

	private void validateWebClient(WebClient.Builder builder, MeterRegistry registry) {
		WebClient webClient = mockWebClient(builder);
		assertThat(registry.find("http.client.requests").meter()).isNull();
		webClient.get().uri("http://example.org/projects/{project}", "spring-boot")
				.exchange().block(Duration.ofSeconds(30));
		assertThat(registry.find("http.client.requests")
				.tags("uri", "/projects/{project}").meter()).isNotNull();
	}

	private WebClient mockWebClient(WebClient.Builder builder) {
		ClientHttpConnector connector = mock(ClientHttpConnector.class);
		given(connector.connect(any(), any(), any()))
				.willReturn(Mono.just(new MockClientHttpResponse(HttpStatus.OK)));
		return builder.clientConnector(connector).build();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTagsProviderConfig {

		@Bean
		public WebClientExchangeTagsProvider customTagsProvider() {
			return mock(WebClientExchangeTagsProvider.class);
		}

	}

}
