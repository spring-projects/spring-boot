/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityRequestMatcherAutoConfiguration.SecurityMvcRequestMatcherConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityRequestMatcherAutoConfiguration}.
 *
 * @author Wang Zhiyang
 */
public class SecurityRequestMatcherAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, SecurityMvcRequestMatcherConfiguration.class));

	@Test
	void testSecurityMvcRequestMatcherConfiguration() {
		this.contextRunner.withUserConfiguration(TestExactlyOneDispatcherServletConfig.class)
			.withPropertyValues("spring.mvc.servlet.path=/custom")
			.run((context) -> {
				MvcRequestMatcher.Builder builder = context.getBean(MvcRequestMatcher.Builder.class);
				assertThat(builder).isNotNull();
				MvcRequestMatcher mvcRequestMatcher = builder.pattern("/example");
				assertThat(mvcRequestMatcher).extracting("servletPath").isEqualTo("/custom");
				assertThat(mvcRequestMatcher).extracting("pattern").isEqualTo("/example");
			});
	}

	@Test
	void testMultipleDispatcherServletMvcRequestMatcherConfiguration() {
		this.contextRunner.withUserConfiguration(TestMultipleDispatcherServletConfig.class)
			.run((context) -> assertThat(context.containsBean("mvcRequestMatcherBuilder")).isFalse());
	}

	@Test
	void testPrototypeDispatcherServletMvcRequestMatcherConfiguration() {
		this.contextRunner.withUserConfiguration(TestPrototypeDispatcherServletConfig.class)
			.run((context) -> assertThat(context.containsBean("mvcRequestMatcherBuilder")).isFalse());
	}

	@Test
	void testNoDispatcherServletMvcRequestMatcherConfiguration() {
		this.contextRunner.run((context) -> assertThat(context.containsBean("mvcRequestMatcherBuilder")).isFalse());
	}

	@Configuration
	static class TestExactlyOneDispatcherServletConfig {

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration
	static class TestMultipleDispatcherServletConfig {

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean("dispatcherServletTwo")
		DispatcherServlet dispatcherServletTwo() {
			return new DispatcherServlet();
		}

	}

	@Configuration
	static class TestPrototypeDispatcherServletConfig {

		@Bean
		@Scope("prototype")
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

}
