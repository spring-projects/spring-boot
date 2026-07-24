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
import org.mockito.Mockito;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for
 * {@link ConditionalOnDefaultReactiveWebSecurity @ConditionalOnDefaultReactiveWebSecurity}.
 *
 * @author Michael Wirth
 **/
class ConditionalOnDefaultReactiveWebSecurityTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void testConditionalOnDefaultReactiveWebSecurityWithoutSecurityConfiguration() {
		this.contextRunner.withUserConfiguration(OnDefaultWebSecurityConfiguration.class).run(this::hasBarBean);
	}

	@Test
	void testConditionalOnDefaultReactiveWebSecurityWithSecurityConfiguration() {
		this.contextRunner
			.withUserConfiguration(SecurityWebFilterChainConfiguration.class, OnDefaultWebSecurityConfiguration.class)
			.run(this::doesNotHaveBarBean);
	}

	private void hasBarBean(AssertableApplicationContext context) {
		assertThat(context).hasBean("bar");
		assertThat(context.getBean("bar")).isEqualTo("bar");
	}

	private void doesNotHaveBarBean(AssertableApplicationContext context) {
		assertThat(context).doesNotHaveBean("bar");
	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityWebFilterChainConfiguration {

		@Bean
		SecurityWebFilterChain securityWebFilterChain() {
			return Mockito.mock(SecurityWebFilterChain.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultReactiveWebSecurity
	static class OnDefaultWebSecurityConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

}
