/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnWarDeployment @ConditionalOnWarDeployment}.
 *
 * @author Madhura Bhave
 */
class ConditionalOnWarDeploymentTests {

	@Test
	void nonWebApplicationShouldNotMatch() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void reactiveWebApplicationShouldNotMatch() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void embeddedServletWebApplicationShouldNotMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebApplicationContext::new);
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("forWar"));
	}

	@Test
	void warDeployedServletWebApplicationShouldMatch() {
		// sets a mock servletContext before context refresh which is what the
		// SpringBootServletInitializer does for WAR deployments.
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();
		contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).hasBean("forWar"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWarDeployment
	static class TestConfiguration {

		@Bean
		String forWar() {
			return "forWar";
		}

	}

}
