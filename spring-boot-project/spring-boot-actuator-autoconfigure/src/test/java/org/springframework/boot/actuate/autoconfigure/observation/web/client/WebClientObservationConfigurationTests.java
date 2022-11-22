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

package org.springframework.boot.actuate.autoconfigure.observation.web.client;

import java.time.Duration;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.ObservationWebClientCustomizer;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
 * Tests for {@link WebClientObservationConfiguration}
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("removal")
class WebClientObservationConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withBean(ObservationRegistry.class, TestObservationRegistry::create)
			.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
					WebClientAutoConfiguration.class, HttpClientObservationsAutoConfiguration.class));

	@Test
	void contributesCustomizerBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ObservationWebClientCustomizer.class)
				.doesNotHaveBean(DefaultWebClientExchangeTagsProvider.class));
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
	void shouldNotOverrideCustomTagsProvider() {
		this.contextRunner.withUserConfiguration(CustomTagsProviderConfig.class).run((context) -> assertThat(context)
				.getBeans(WebClientExchangeTagsProvider.class).hasSize(1).containsKey("customTagsProvider"));
	}

	@Test
	void shouldUseCustomConventionIfAvailable() {
		this.contextRunner.withUserConfiguration(CustomConvention.class).run((context) -> {
			TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
			WebClient.Builder builder = context.getBean(WebClient.Builder.class);
			WebClient webClient = mockWebClient(builder);
			TestObservationRegistryAssert.assertThat(registry).doesNotHaveAnyObservation();
			webClient.get().uri("https://example.org/projects/{project}", "spring-boot").retrieve().toBodilessEntity()
					.block(Duration.ofSeconds(30));
			TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("http.client.requests")
					.that().hasLowCardinalityKeyValue("project", "spring-boot");
		});
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		this.contextRunner.withPropertyValues("management.metrics.web.client.max-uri-tags=2").run((context) -> {
			TestObservationRegistry registry = getInitializedRegistry(context);
			// TODO check size is 2
			TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("http.client.requests");
			assertThat(output).contains("Reached the maximum number of URI tags for 'http.client.requests'.")
					.contains("Are you using 'uriVariables'?");
		});
	}

	@Test
	void shouldNotDenyNorLogIfMaxUrisIsNotReached(CapturedOutput output) {
		this.contextRunner.withPropertyValues("management.metrics.web.client.max-uri-tags=5").run((context) -> {
			TestObservationRegistry registry = getInitializedRegistry(context);
			// TODO check size is 3
			TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("http.client.requests");
			assertThat(output).doesNotContain("Reached the maximum number of URI tags for 'http.client.requests'.")
					.doesNotContain("Are you using 'uriVariables'?");
		});
	}

	private TestObservationRegistry getInitializedRegistry(AssertableApplicationContext context) {
		WebClient webClient = mockWebClient(context.getBean(WebClient.Builder.class));
		TestObservationRegistry registry = context.getBean(TestObservationRegistry.class);
		for (int i = 0; i < 3; i++) {
			webClient.get().uri("https://example.org/projects/" + i).retrieve().toBodilessEntity()
					.block(Duration.ofSeconds(30));
		}
		return registry;
	}

	private void validateWebClient(WebClient.Builder builder, TestObservationRegistry registry) {
		WebClient webClient = mockWebClient(builder);
		TestObservationRegistryAssert.assertThat(registry).doesNotHaveAnyObservation();
		webClient.get().uri("https://example.org/projects/{project}", "spring-boot").retrieve().toBodilessEntity()
				.block(Duration.ofSeconds(30));
		TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("http.client.requests").that()
				.hasLowCardinalityKeyValue("uri", "https://example.org/projects/{project}");
	}

	private WebClient mockWebClient(WebClient.Builder builder) {
		ClientHttpConnector connector = mock(ClientHttpConnector.class);
		given(connector.connect(any(), any(), any())).willReturn(Mono.just(new MockClientHttpResponse(HttpStatus.OK)));
		return builder.clientConnector(connector).build();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTagsProviderConfig {

		@Bean
		WebClientExchangeTagsProvider customTagsProvider() {
			return mock(WebClientExchangeTagsProvider.class);
		}

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

}
