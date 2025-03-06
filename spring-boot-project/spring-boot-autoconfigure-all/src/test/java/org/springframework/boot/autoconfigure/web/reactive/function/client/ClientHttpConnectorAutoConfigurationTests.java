/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ClientHttpConnector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClientHttpConnectorAutoConfiguration}
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
class ClientHttpConnectorAutoConfigurationTests {

	@Test
	void shouldApplyReactorNettyHttpClientMapper() {
		new ReactiveWebApplicationContextRunner().withConfiguration(AutoConfigurations.of(
				ClientHttpConnectorAutoConfiguration.class,
				org.springframework.boot.autoconfigure.http.client.reactive.ClientHttpConnectorAutoConfiguration.class))
			.withUserConfiguration(CustomReactorNettyHttpClientMapper.class)
			.run((context) -> {
				context.getBean(ClientHttpConnector.class);
				assertThat(CustomReactorNettyHttpClientMapper.called).isTrue();
			});
	}

	static class CustomReactorNettyHttpClientMapper {

		static boolean called = false;

		@Bean
		ReactorNettyHttpClientMapper clientMapper() {
			return (client) -> {
				called = true;
				return client.baseUrl("/test");
			};
		}

	}

}
