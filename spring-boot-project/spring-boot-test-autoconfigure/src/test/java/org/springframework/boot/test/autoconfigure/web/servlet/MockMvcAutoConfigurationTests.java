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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockMvcAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Brian Clozel
 */
class MockMvcAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MockMvcAutoConfiguration.class));

	@Test
	void registersDispatcherServletFromMockMvc() {
		this.contextRunner.run((context) -> {
			MockMvc mockMvc = context.getBean(MockMvc.class);
			assertThat(context).hasSingleBean(DispatcherServlet.class);
			assertThat(context.getBean(DispatcherServlet.class)).isEqualTo(mockMvc.getDispatcherServlet());
		});
	}

	@Test
	void registersWebTestClient() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(WebTestClient.class));
	}

	@Test
	void shouldNotRegisterWebTestClientIfWebFluxMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(WebClient.class))
			.run((context) -> assertThat(context).doesNotHaveBean(WebTestClient.class));
	}

	@Test
	void shouldApplyWebTestClientCustomizers() {
		this.contextRunner.withUserConfiguration(WebTestClientCustomConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(WebTestClient.class);
			assertThat(context).hasBean("myWebTestClientCustomizer");
			then(context.getBean("myWebTestClientCustomizer", WebTestClientBuilderCustomizer.class)).should()
				.customize(any(WebTestClient.Builder.class));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class WebTestClientCustomConfig {

		@Bean
		WebTestClientBuilderCustomizer myWebTestClientCustomizer() {
			return mock(WebTestClientBuilderCustomizer.class);
		}

	}

}
