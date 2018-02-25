/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.integration;

import org.junit.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.management.graph.Graph;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IntegrationGraphEndpoint}.
 *
 * @author Tim Ysewyn
 */
public class IntegrationGraphEndpointTests {

	@Test
	public void shouldReturnEmptyGraph() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			Graph graph = context.getBean(IntegrationGraphEndpoint.class).graph();
			assertContentDescriptor(graph);
            assertThat(graph.getNodes()).isEmpty();
            assertThat(graph.getLinks()).isEmpty();
		});
	}

	@Test
	public void shouldReturnGraph() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class, IntegrationConfiguration.class);
		contextRunner.run((context) -> {
			Graph graph = context.getBean(IntegrationGraphEndpoint.class).graph();
			assertContentDescriptor(graph);
            assertThat(graph.getNodes()).hasSize(3);
            assertThat(graph.getLinks()).hasSize(1);
		});
	}

	@Test
	public void shouldRebuildGraph() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			context.getBean(IntegrationGraphEndpoint.class).rebuild();
		});
	}

	private void assertContentDescriptor(Graph graph) {
		Map<String, Object> contentDescriptor = graph.getContentDescriptor();
		assertThat(contentDescriptor).isNotEmpty();
		assertThat(contentDescriptor).containsOnlyKeys("provider", "providerFormatVersion", "providerVersion");
		assertThat(contentDescriptor.get("provider")).isEqualTo("spring-integration");
		assertThat(contentDescriptor.get("providerFormatVersion")).isEqualTo(1.0f);
		assertThat(contentDescriptor.get("providerVersion")).isEqualTo("5.0.2.RELEASE");
	}

	@Configuration
	public static class EndpointConfiguration {

		@Bean
		public IntegrationGraphEndpoint endpoint(ConfigurableApplicationContext context) {
			return new IntegrationGraphEndpoint(context);
		}

	}

	@Configuration
	@EnableIntegration
	static class IntegrationConfiguration {

	}

}
