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

package org.springframework.boot.reactor.autoconfigure;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.reactive.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ForwardedHeaderAutoConfigurationTests {

	@Test
	void forwardedHeaderTransformerShouldBeConfigured() {
		try (AnnotationConfigReactiveWebApplicationContext context = load(
				"server.forward-headers-strategy=framework")) {
			assertThat(context.getBean(ForwardedHeaderTransformer.class)).isNotNull();
		}
	}

	@Test
	void forwardedHeaderTransformerWhenStrategyNotFilterShouldNotBeConfigured() {
		try (AnnotationConfigReactiveWebApplicationContext context = load("server.forward-headers-strategy=native")) {
			assertThatExceptionOfType(BeansException.class)
				.isThrownBy(() -> context.getBean(ForwardedHeaderTransformer.class));
		}
	}

	@Test
	void forwardedHeaderTransformerWhenNoStrategyGivenShouldNotBeConfigured() {
		try (AnnotationConfigReactiveWebApplicationContext context = load()) {
			assertThatExceptionOfType(BeansException.class)
				.isThrownBy(() -> context.getBean(ForwardedHeaderTransformer.class));
		}
	}

	@Test
	void forwardedHeaderTransformerWhenTransformerAlreadyRegisteredShouldBackOff() {
		try (AnnotationConfigReactiveWebApplicationContext context = load(TransformerConfiguration.class,
				"server.forward-headers-strategy=framework")) {
			final String[] forwardedHeaderTransformerBeannames = context
				.getBeanNamesForType(ForwardedHeaderTransformer.class);
			assertThat(List.of(forwardedHeaderTransformerBeannames)).isEqualTo(List.of("myForwardedHeaderTransformer"));
		}
	}

	private AnnotationConfigReactiveWebApplicationContext load(final String... environment) {
		return load((Class<?>) null, environment);
	}

	private AnnotationConfigReactiveWebApplicationContext load(@Nullable final Class<?> config,
			final String... environment) {
		final List<Class<?>> configs = (config != null) ? List.of(config) : null;
		return load(configs, environment);
	}

	private AnnotationConfigReactiveWebApplicationContext load(@Nullable final List<Class<?>> configs,
			final String... environment) {
		final AnnotationConfigReactiveWebApplicationContext applicationContext = new AnnotationConfigReactiveWebApplicationContext();
		TestPropertyValues.of(environment).applyTo(applicationContext);
		Optional.ofNullable(configs).map((c) -> c.toArray(new Class<?>[0])).ifPresent(applicationContext::register);
		applicationContext.register(ForwardedHeaderAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class TransformerConfiguration {

		@Bean("myForwardedHeaderTransformer")
		ForwardedHeaderTransformer myForwardedHeaderTransformer() {
			return new ForwardedHeaderTransformer();
		}

	}

}
