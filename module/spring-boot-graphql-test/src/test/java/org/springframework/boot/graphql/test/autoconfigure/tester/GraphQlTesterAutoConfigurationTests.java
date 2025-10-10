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

package org.springframework.boot.graphql.test.autoconfigure.tester;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GraphQlTesterAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlTesterAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, GraphQlTesterAutoConfiguration.class));

	@Test
	void shouldNotContributeTesterIfGraphQlServiceNotPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(GraphQlTester.class));
	}

	@Test
	void shouldContributeTester() {
		this.contextRunner.withUserConfiguration(CustomGraphQlServiceConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed().hasSingleBean(GraphQlTester.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomGraphQlServiceConfiguration {

		@Bean
		ExecutionGraphQlService graphQlService() {
			return mock(ExecutionGraphQlService.class);
		}

	}

}
