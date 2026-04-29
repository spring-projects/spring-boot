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

package org.springframework.boot.servlet.autoconfigure;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.Filter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ForwardedHeaderAutoConfigurationTests {

	@Test
	void forwardedHeaderFilterShouldBeConfigured() {
		try (AnnotationConfigWebApplicationContext context = load("server.forward-headers-strategy=framework")) {
			final FilterRegistrationBean<?> filterRegistrationBean = context.getBean(FilterRegistrationBean.class);
			final Filter filter = filterRegistrationBean.getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
		}
	}

	@Test
	void forwardedHeaderFilterShouldBeConfiguredAndCustomizerApplied() {
		try (AnnotationConfigWebApplicationContext context = load(CustomizerConfiguration.class,
				"server.forward-headers-strategy=framework")) {
			final FilterRegistrationBean<?> filterRegistrationBean = context.getBean(FilterRegistrationBean.class);
			final Filter filter = filterRegistrationBean.getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(true);
		}
	}

	@Test
	void forwardedHeaderFilterWhenStrategyNotFilterShouldNotBeConfigured() {
		try (AnnotationConfigWebApplicationContext context = load("server.forward-headers-strategy=native")) {
			assertThatExceptionOfType(BeansException.class)
				.isThrownBy(() -> context.getBean(FilterRegistrationBean.class));
		}
	}

	@Test
	void forwardedHeaderFilterWhenNoStrategyGivenShouldNotBeConfigured() {
		try (AnnotationConfigWebApplicationContext context = load()) {
			assertThatExceptionOfType(BeansException.class)
				.isThrownBy(() -> context.getBean(FilterRegistrationBean.class));
		}
	}

	@Test
	void forwardedHeaderFilterWhenFilterAlreadyRegisteredShouldBackOff() {
		try (AnnotationConfigWebApplicationContext context = load(FilterConfiguration.class,
				"server.forward-headers-strategy=framework")) {
			final String[] forwardedHeaderFilterBeanNames = context.getBeanNamesForType(FilterRegistrationBean.class);
			assertThat(List.of(forwardedHeaderFilterBeanNames)).isEqualTo(List.of("myForwardedHeaderFilter"));
		}
	}

	@Test
	void forwardedHeaderFilterWhenFilterAlreadyRegisteredCustomizerShouldNotBeApplied() {
		try (AnnotationConfigWebApplicationContext context = load(
				List.of(FilterConfiguration.class, CustomizerConfiguration.class),
				"server.forward-headers-strategy=framework")) {
			final FilterRegistrationBean<?> filterRegistrationBean = context.getBean(FilterRegistrationBean.class);
			final Filter filter = filterRegistrationBean.getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
		}
	}

	private AnnotationConfigWebApplicationContext load(final String... environment) {
		return load((Class<?>) null, environment);
	}

	private AnnotationConfigWebApplicationContext load(@Nullable final Class<?> config, final String... environment) {
		final List<Class<?>> configs = (config != null) ? List.of(config) : null;
		return load(configs, environment);
	}

	private AnnotationConfigWebApplicationContext load(@Nullable final List<Class<?>> configs,
			final String... environment) {
		final AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of(environment).applyTo(applicationContext);
		Optional.ofNullable(configs).map((c) -> c.toArray(new Class<?>[0])).ifPresent(applicationContext::register);
		applicationContext.register(ForwardedHeaderAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class FilterConfiguration {

		@Bean("myForwardedHeaderFilter")
		FilterRegistrationBean<ForwardedHeaderFilter> myForwardedHeaderFilter() {
			return new FilterRegistrationBean<>(new ForwardedHeaderFilter());
		}

	}

	@Configuration
	static class CustomizerConfiguration {

		@Bean
		ForwardedHeaderFilterCustomizer customizer() {
			return (f) -> f.setRelativeRedirects(true);
		}

	}

}
