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

package org.springframework.boot.autoconfigure.graphql.rsocket;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.rsocket.RSocketRequesterAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.RSocketGraphQlClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketGraphQlClientAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class RSocketGraphQlClientAutoConfigurationTests {

	private static final RSocketGraphQlClient.Builder<?> builderInstance = RSocketGraphQlClient.builder();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RSocketStrategiesAutoConfiguration.class,
				RSocketRequesterAutoConfiguration.class, RSocketGraphQlClientAutoConfiguration.class));

	@Test
	void shouldCreateBuilder() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RSocketGraphQlClient.Builder.class));
	}

	@Test
	void shouldGetPrototypeScopedBean() {
		this.contextRunner.run((context) -> {
			RSocketGraphQlClient.Builder<?> first = context.getBean(RSocketGraphQlClient.Builder.class);
			RSocketGraphQlClient.Builder<?> second = context.getBean(RSocketGraphQlClient.Builder.class);
			assertThat(first).isNotEqualTo(second);
		});
	}

	@Test
	void shouldNotCreateBuilderIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(CustomRSocketGraphQlClientBuilder.class).run((context) -> {
			RSocketGraphQlClient.Builder<?> builder = context.getBean(RSocketGraphQlClient.Builder.class);
			assertThat(builder).isEqualTo(builderInstance);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRSocketGraphQlClientBuilder {

		@Bean
		RSocketGraphQlClient.Builder<?> myRSocketGraphQlClientBuilder() {
			return builderInstance;
		}

	}

}
