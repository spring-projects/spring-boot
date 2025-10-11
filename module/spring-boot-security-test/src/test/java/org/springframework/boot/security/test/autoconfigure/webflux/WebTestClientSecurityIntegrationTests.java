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

package org.springframework.boot.security.test.autoconfigure.webflux;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webtestclient.WebTestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for mocked-infrastructure-backed WebTestClient security.
 *
 * @author Andy Wilkinson
 */
class WebTestClientSecurityIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(WebTestClientAutoConfiguration.class, SecurityWebTestClientAutoConfiguration.class));

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplySpringSecurityConfigurer() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			WebTestClient.Builder builder = (WebTestClient.Builder) ReflectionTestUtils.getField(webTestClient,
					"builder");
			WebHttpHandlerBuilder httpHandlerBuilder = (WebHttpHandlerBuilder) ReflectionTestUtils.getField(builder,
					"httpHandlerBuilder");
			List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils.getField(httpHandlerBuilder, "filters");
			assertThat(filters.get(0).getClass().getName()).isEqualTo(
					"org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers$MutatorFilter");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		WebHandler webHandler() {
			return mock(WebHandler.class);
		}

	}

}
