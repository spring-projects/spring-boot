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

package org.springframework.boot.actuate.autoconfigure.metrics.web.reactive;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.metrics.web.TestController;
import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.server.MetricsWebFilter;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebFluxMetricsAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Dmytro Nosan
 */
public class WebFluxMetricsAutoConfigurationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.with(MetricsRun.simple()).withConfiguration(
					AutoConfigurations.of(WebFluxMetricsAutoConfiguration.class));

	@Rule
	public final OutputCapture output = new OutputCapture();

	@Test
	public void shouldProvideWebFluxMetricsBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).getBeans(MetricsWebFilter.class).hasSize(1);
			assertThat(context).getBeans(DefaultWebFluxTagsProvider.class).hasSize(1);
		});
	}

	@Test
	public void shouldNotOverrideCustomTagsProvider() {
		this.contextRunner.withUserConfiguration(CustomWebFluxTagsProviderConfig.class)
				.run((context) -> assertThat(context).getBeans(WebFluxTagsProvider.class)
						.hasSize(1).containsKey("customWebFluxTagsProvider"));
	}

	@Test
	public void afterMaxUrisReachedFurtherUrisAreDenied() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
				.withUserConfiguration(TestController.class)
				.withPropertyValues("management.metrics.web.server.max-uri-tags=2")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.server.requests").meters()).hasSize(2);
					assertThat(this.output.toString())
							.contains("Reached the maximum number of URI tags "
									+ "for 'http.server.requests'");
				});
	}

	@Test
	public void shouldNotDenyNorLogIfMaxUrisIsNotReached() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
				.withUserConfiguration(TestController.class)
				.withPropertyValues("management.metrics.web.server.max-uri-tags=5")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.server.requests").meters()).hasSize(3);
					assertThat(this.output.toString()).doesNotContain(
							"Reached the maximum number of URI tags for 'http.server.requests'");
				});
	}

	@Test
	public void metricsAreNotRecordedIfAutoTimeRequestsIsDisabled() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class))
				.withUserConfiguration(TestController.class)
				.withPropertyValues(
						"management.metrics.web.server.auto-time-requests=false")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.find("http.server.requests").meter()).isNull();
				});
	}

	private MeterRegistry getInitializedMeterRegistry(
			AssertableReactiveWebApplicationContext context) {
		WebTestClient webTestClient = WebTestClient.bindToApplicationContext(context)
				.build();
		for (int i = 0; i < 3; i++) {
			webTestClient.get().uri("/test" + i).exchange().expectStatus().isOk();
		}
		return context.getBean(MeterRegistry.class);
	}

	@Configuration(proxyBeanMethods = false)
	protected static class CustomWebFluxTagsProviderConfig {

		@Bean
		public WebFluxTagsProvider customWebFluxTagsProvider() {
			return mock(WebFluxTagsProvider.class);
		}

	}

}
