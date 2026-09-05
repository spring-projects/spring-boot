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

package org.springframework.boot.security.autoconfigure.web.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnDefaultReactiveWebSecurity}.
 *
 * @author Andy Wilkinson
 */
class ConditionalOnDefaultReactiveWebSecurityTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void matchesWithoutSecurityWebFilterChainBean() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TestConfiguration.class))
			.run((context) -> assertThat(context).hasBean("testBean"));
	}

	@Test
	void doesNotMatchWhenSecurityWebFilterChainBeanIsDefined() {
		this.contextRunner.withBean(SecurityWebFilterChain.class, () -> mock(SecurityWebFilterChain.class))
			.withConfiguration(AutoConfigurations.of(TestConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean("testBean"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultReactiveWebSecurity
	static class TestConfiguration {

		@Bean
		String testBean() {
			return "test";
		}

	}

}
