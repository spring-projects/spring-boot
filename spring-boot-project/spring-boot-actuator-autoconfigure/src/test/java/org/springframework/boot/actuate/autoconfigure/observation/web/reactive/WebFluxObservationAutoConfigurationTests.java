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

package org.springframework.boot.actuate.autoconfigure.observation.web.reactive;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.reactive.server.DefaultWebFluxTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.reactive.ServerHttpObservationFilter;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxObservationAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Dmytro Nosan
 * @author Madhura Bhave
 */
@ExtendWith(OutputCaptureExtension.class)
@SuppressWarnings("removal")
class WebFluxObservationAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.with(MetricsRun.simple()).withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
					WebFluxObservationAutoConfiguration.class));

	@Test
	void shouldProvideWebFluxObservationFilter() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ServerHttpObservationFilter.class));
	}

	@Test
	void shouldUseConventionAdapterWhenCustomTagsProvider() {
		this.contextRunner.withUserConfiguration(CustomTagsProviderConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ServerHttpObservationFilter.class);
			assertThat(context).hasSingleBean(WebFluxTagsProvider.class);
			assertThat(context).getBean(ServerHttpObservationFilter.class).extracting("observationConvention")
					.isInstanceOf(ServerRequestObservationConventionAdapter.class);
		});
	}

	@Test
	void shouldUseConventionAdapterWhenCustomTagsContributor() {
		this.contextRunner.withUserConfiguration(CustomTagsContributorConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ServerHttpObservationFilter.class);
			assertThat(context).hasSingleBean(WebFluxTagsContributor.class);
			assertThat(context).getBean(ServerHttpObservationFilter.class).extracting("observationConvention")
					.isInstanceOf(ServerRequestObservationConventionAdapter.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTagsProviderConfiguration {

		@Bean
		WebFluxTagsProvider tagsProvider() {
			return new DefaultWebFluxTagsProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTagsContributorConfiguration {

		@Bean
		WebFluxTagsContributor tagsContributor() {
			return new CustomTagsContributor();
		}

	}

	static class CustomTagsContributor implements WebFluxTagsContributor {

		@Override
		public Iterable<Tag> httpRequestTags(ServerWebExchange exchange, Throwable ex) {
			return Tags.of("custom", "testvalue");
		}

	}

}
