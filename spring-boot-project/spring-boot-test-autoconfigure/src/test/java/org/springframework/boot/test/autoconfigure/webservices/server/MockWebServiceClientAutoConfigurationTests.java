/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.server;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.ws.test.server.MockWebServiceClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockWebServiceClientAutoConfiguration}.
 *
 * @author Daniil Razorenov
 */
class MockWebServiceClientAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MockWebServiceClientAutoConfiguration.class));

	@Test
	void shouldRegisterMockWebServiceClient() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MockWebServiceClient.class));
	}

	@Test
	void shouldNotRegisterMockWebServiceClientWhenItIsNotOnTheClasspath() {
		FilteredClassLoader classLoader = new FilteredClassLoader(MockWebServiceClient.class);

		this.contextRunner.withClassLoader(classLoader)
				.run((context) -> assertThat(context).doesNotHaveBean(MockWebServiceClient.class));
	}

}
