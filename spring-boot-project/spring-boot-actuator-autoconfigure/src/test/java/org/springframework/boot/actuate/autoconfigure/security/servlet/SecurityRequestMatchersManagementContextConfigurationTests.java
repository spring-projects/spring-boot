/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityRequestMatchersManagementContextConfiguration}.
 *
 * @author Madhura Bhave
 */
class SecurityRequestMatchersManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class));

	@Test
	void configurationConditionalOnWebApplication() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class))
			.withUserConfiguration(TestMvcConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(RequestMatcherProvider.class));
	}

	@Test
	void configurationConditionalOnRequestMatcherClass() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader("org.springframework.security.web.util.matcher.RequestMatcher"))
			.run((context) -> assertThat(context).doesNotHaveBean(RequestMatcherProvider.class));
	}

	@Test
	void registersRequestMatcherProviderIfMvcPresent() {
		this.contextRunner.withUserConfiguration(TestMvcConfiguration.class).run((context) -> {
			PathPatternRequestMatcherProvider matcherProvider = context
				.getBean(PathPatternRequestMatcherProvider.class);
			RequestMatcher requestMatcher = matcherProvider.getRequestMatcher("/example", null);
			assertThat(requestMatcher).extracting("pattern")
				.isEqualTo(PathPatternParser.defaultInstance.parse("/custom/example"));
		});
	}

	@Test
	void registersRequestMatcherForJerseyProviderIfJerseyPresentAndMvcAbsent() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
			.withUserConfiguration(TestJerseyConfiguration.class)
			.run((context) -> {
				PathPatternRequestMatcherProvider matcherProvider = context
					.getBean(PathPatternRequestMatcherProvider.class);
				RequestMatcher requestMatcher = matcherProvider.getRequestMatcher("/example", null);
				assertThat(requestMatcher).extracting("pattern")
					.isEqualTo(PathPatternParser.defaultInstance.parse("/admin/example"));
			});
	}

	@Test
	void mvcRequestMatcherProviderConditionalOnDispatcherServletClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
			.run((context) -> assertThat(context).doesNotHaveBean(PathPatternRequestMatcherProvider.class));
	}

	@Test
	void mvcRequestMatcherProviderConditionalOnDispatcherServletPathBean() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(PathPatternRequestMatcherProvider.class));
	}

	@Test
	void jerseyRequestMatcherProviderConditionalOnResourceConfigClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.glassfish.jersey.server.ResourceConfig"))
			.run((context) -> assertThat(context).doesNotHaveBean(PathPatternRequestMatcherProvider.class));
	}

	@Test
	void jerseyRequestMatcherProviderConditionalOnJerseyApplicationPathBean() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class))
			.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
			.run((context) -> assertThat(context).doesNotHaveBean(PathPatternRequestMatcherProvider.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestMvcConfiguration {

		@Bean
		DispatcherServletPath dispatcherServletPath() {
			return () -> "/custom";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestJerseyConfiguration {

		@Bean
		JerseyApplicationPath jerseyApplicationPath() {
			return () -> "/admin";
		}

	}

}
