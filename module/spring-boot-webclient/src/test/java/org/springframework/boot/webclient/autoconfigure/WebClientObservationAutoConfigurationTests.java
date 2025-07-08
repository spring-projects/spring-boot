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

package org.springframework.boot.webclient.autoconfigure;

import java.time.Duration;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webclient.observation.ObservationWebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientObservationAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class WebClientObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(ObservationRegistry.class, TestObservationRegistry::create)
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class, WebClientAutoConfiguration.class))
		.withUserConfiguration(WebClientObservationAutoConfiguration.class);

	@Test
	void contributesCustomizerBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ObservationWebClientCustomizer.class));
	}

	@Test
	void webClientCreatedWithBuilderIsInstrumented() {
		this.contextRunner.run((context) -> {
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			validateWebClient(builder, registry);
		});
	}

	@Test
	void shouldUseCustomConventionIfAvailable() {
		this.contextRunner.withUserConfiguration(CustomConvention.class).run((context) -> {
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			WebClient webClient = mockWebClient(builder);
			assertThat(registry).doesNotHaveAnyObservation();
			webClient.get()
				.uri("https://example.org/projects/{project}", "spring-boot")
				.retrieve()
				.toBodilessEntity()
				.block(Duration.ofSeconds(30));
			assertThat(registry).hasObservationWithNameEqualTo("http.client.requests")
				.that()
				.hasLowCardinalityKeyValue("project", "spring-boot");
		});
	}

	private void validateWebClient(WebClient.Builder builder, TestObservationRegistry registry) {
		WebClient webClient = mockWebClient(builder);
		assertThat(registry).doesNotHaveAnyObservation();
		webClient.get()
			.uri("https://example.org/projects/{project}", "spring-boot")
			.retrieve()
			.toBodilessEntity()
			.block(Duration.ofSeconds(30));
		assertThat(registry).hasObservationWithNameEqualTo("http.client.requests")
			.that()
			.hasLowCardinalityKeyValue("uri", "/projects/{project}");
	}

	private WebClient mockWebClient(WebClient.Builder builder) {
		ClientHttpConnector connector = mock(ClientHttpConnector.class);
		given(connector.connect(any(), any(), any())).willReturn(Mono.just(new MockClientHttpResponse(HttpStatus.OK)));
		return builder.clientConnector(connector).build();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConventionConfig {

		@Bean
		CustomConvention customConvention() {
			return new CustomConvention();
		}

	}

	static class CustomConvention extends DefaultClientRequestObservationConvention {

		@Override
		public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
			return super.getLowCardinalityKeyValues(context).and("project", "spring-boot");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MetricsConfiguration {

		@Bean
		MeterObservationHandler<Context> meterObservationHandler(MeterRegistry registry) {
			return new DefaultMeterObservationHandler(registry);
		}

	}

}
