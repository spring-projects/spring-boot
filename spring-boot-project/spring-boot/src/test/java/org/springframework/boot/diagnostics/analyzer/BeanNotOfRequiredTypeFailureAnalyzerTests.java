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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link BeanNotOfRequiredTypeFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class BeanNotOfRequiredTypeFailureAnalyzerTests {

	private final FailureAnalyzer analyzer = new BeanNotOfRequiredTypeFailureAnalyzer();

	@Test
	void jdkProxyCausesInjectionFailure() {
		FailureAnalysis analysis = performAnalysis(JdkProxyConfiguration.class);
		assertThat(analysis.getDescription()).startsWith("The bean 'asyncBean'");
		assertThat(analysis.getDescription()).contains("'" + AsyncBean.class.getName() + "'");
		assertThat(analysis.getDescription()).endsWith(String.format("%s%n", SomeInterface.class.getName()));
	}

	private FailureAnalysis performAnalysis(Class<?> configuration) {
		FailureAnalysis analysis = this.analyzer.analyze(createFailure(configuration));
		assertThat(analysis).isNotNull();
		return analysis;
	}

	private Exception createFailure(Class<?> configuration) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(configuration)) {
			fail("Expected failure did not occur");
			return null;
		}
		catch (Exception ex) {
			return ex;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAsync
	@Import(UserConfiguration.class)
	static class JdkProxyConfiguration {

		@Bean
		AsyncBean asyncBean() {
			return new AsyncBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UserConfiguration {

		@Bean
		AsyncBeanUser user(AsyncBean bean) {
			return new AsyncBeanUser(bean);
		}

	}

	static class AsyncBean implements SomeInterface {

		@Async
		void foo() {

		}

		@Override
		public void bar() {

		}

	}

	interface SomeInterface {

		void bar();

	}

	static class AsyncBeanUser {

		AsyncBeanUser(AsyncBean asyncBean) {
		}

	}

}
