/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.server.MetricsWebFilter;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
					SimpleMetricsExportAutoConfiguration.class,
					WebFluxMetricsAutoConfiguration.class));

	@Rule
	public OutputCapture output = new OutputCapture();

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
					WebTestClient webTestClient = WebTestClient
							.bindToApplicationContext(context).build();

					for (int i = 0; i < 3; i++) {
						webTestClient.get().uri("/test" + i).exchange().expectStatus()
								.isOk();
					}
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.get("http.server.requests").meters()).hasSize(2);
					assertThat(this.output.toString())
							.contains("Reached the maximum number of URI tags "
									+ "for 'http.server.requests'");
				});
	}

	@Configuration
	protected static class CustomWebFluxTagsProviderConfig {

		@Bean
		public WebFluxTagsProvider customWebFluxTagsProvider() {
			return mock(WebFluxTagsProvider.class);
		}

	}

	@RestController
	static class TestController {

		@GetMapping("test0")
		public Mono<String> test0() {
			return Mono.just("test0");
		}

		@GetMapping("test1")
		public Mono<String> test1() {
			return Mono.just("test1");
		}

		@GetMapping("test2")
		public Mono<String> test2() {
			return Mono.just("test2");
		}

	}

}
