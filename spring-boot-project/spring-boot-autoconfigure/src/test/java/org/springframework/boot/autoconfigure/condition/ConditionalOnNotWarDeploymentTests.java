/*
 * Copyright 2012-2023 the original author or authors.
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
 * Tests for {@link ConditionalOnNotWarDeployment @ConditionalOnNotWarDeployment}.
 *
 * @author Guirong Hu
 */
class ConditionalOnNotWarDeploymentTests {

	@Test
	void nonWebApplicationShouldMatch() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner();
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).hasBean("notForWar"));
	}

	@Test
	void reactiveWebApplicationShouldMatch() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner();
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).hasBean("notForWar"));
	}

	@Test
	void embeddedServletWebApplicationShouldMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebApplicationContext::new);
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).hasBean("notForWar"));
	}

	@Test
	void warDeployedServletWebApplicationShouldNotMatch() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();
		contextRunner.withUserConfiguration(NotWarDeploymentConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("notForWar"));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnNotWarDeployment
	static class NotWarDeploymentConfiguration {

		@Bean
		String notForWar() {
			return "notForWar";
		}

	}

}
