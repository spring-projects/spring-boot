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

package org.springframework.boot.actuate.autoconfigure.env;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertyValueDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class EnvironmentEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(EnvironmentEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=env")
			.withSystemProperties("dbPassword=123456", "apiKey=123456")
			.run(validateSystemProperties("******", "******"));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.env.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(EnvironmentEndpoint.class));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(EnvironmentEndpoint.class));
	}

	@Test
	void customSanitizingFunctionsAreAppliedInOrder() {
		this.contextRunner.withUserConfiguration(SanitizingFunctionConfiguration.class)
			.withPropertyValues("management.endpoint.env.show-values: WHEN_AUTHORIZED")
			.withPropertyValues("management.endpoints.web.exposure.include=env")
			.withSystemProperties("custom=123456", "password=123456")
			.run((context) -> {
				assertThat(context).hasSingleBean(EnvironmentEndpoint.class);
				EnvironmentEndpoint endpoint = context.getBean(EnvironmentEndpoint.class);
				EnvironmentDescriptor env = endpoint.environment(null);
				Map<String, PropertyValueDescriptor> systemProperties = getSource("systemProperties", env)
					.getProperties();
				assertThat(systemProperties.get("custom").getValue()).isEqualTo("$$$111$$$");
				assertThat(systemProperties.get("password").getValue()).isEqualTo("$$$222$$$");
			});
	}

	@Test
	@SuppressWarnings("unchecked")
	void rolesCanBeConfiguredViaTheEnvironment() {
		this.contextRunner.withPropertyValues("management.endpoint.env.roles: test")
			.withPropertyValues("management.endpoints.web.exposure.include=env")
			.withSystemProperties("dbPassword=123456", "apiKey=123456")
			.run((context) -> {
				assertThat(context).hasSingleBean(EnvironmentEndpointWebExtension.class);
				EnvironmentEndpointWebExtension endpoint = context.getBean(EnvironmentEndpointWebExtension.class);
				Set<String> roles = (Set<String>) ReflectionTestUtils.getField(endpoint, "roles");
				assertThat(roles).contains("test");
			});
	}

	@Test
	void showValuesCanBeConfiguredViaTheEnvironment() {
		this.contextRunner.withPropertyValues("management.endpoint.env.show-values: WHEN_AUTHORIZED")
			.withPropertyValues("management.endpoints.web.exposure.include=env")
			.withSystemProperties("dbPassword=123456", "apiKey=123456")
			.run((context) -> {
				assertThat(context).hasSingleBean(EnvironmentEndpoint.class);
				assertThat(context).hasSingleBean(EnvironmentEndpointWebExtension.class);
				EnvironmentEndpointWebExtension webExtension = context.getBean(EnvironmentEndpointWebExtension.class);
				EnvironmentEndpoint endpoint = context.getBean(EnvironmentEndpoint.class);
				assertThat(webExtension).extracting("showValues").isEqualTo(Show.WHEN_AUTHORIZED);
				assertThat(endpoint).extracting("showValues").isEqualTo(Show.WHEN_AUTHORIZED);
			});
	}

	@Test
	void runWhenOnlyExposedOverJmxShouldHaveEndpointBeanWithoutWebExtension() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=info", "spring.jmx.enabled=true",
					"management.endpoints.jmx.exposure.include=env")
			.run((context) -> assertThat(context).hasSingleBean(EnvironmentEndpoint.class)
				.doesNotHaveBean(EnvironmentEndpointWebExtension.class));
	}

	private ContextConsumer<AssertableApplicationContext> validateSystemProperties(String dbPassword, String apiKey) {
		return (context) -> {
			assertThat(context).hasSingleBean(EnvironmentEndpoint.class);
			EnvironmentEndpoint endpoint = context.getBean(EnvironmentEndpoint.class);
			EnvironmentDescriptor env = endpoint.environment(null);
			Map<String, PropertyValueDescriptor> systemProperties = getSource("systemProperties", env).getProperties();
			assertThat(systemProperties.get("dbPassword").getValue()).isEqualTo(dbPassword);
			assertThat(systemProperties.get("apiKey").getValue()).isEqualTo(apiKey);
		};
	}

	private PropertySourceDescriptor getSource(String name, EnvironmentDescriptor descriptor) {
		return descriptor.getPropertySources()
			.stream()
			.filter((source) -> name.equals(source.getName()))
			.findFirst()
			.get();
	}

	@Configuration(proxyBeanMethods = false)
	static class SanitizingFunctionConfiguration {

		@Bean
		@Order(0)
		SanitizingFunction firstSanitizingFunction() {
			return (data) -> {
				if (data.getKey().contains("custom")) {
					return data.withValue("$$$111$$$");
				}
				return data;
			};
		}

		@Bean
		@Order(1)
		SanitizingFunction secondSanitizingFunction() {
			return (data) -> {
				if (data.getKey().contains("custom") || data.getKey().contains("password")) {
					return data.withValue("$$$222$$$");
				}
				return data;
			};
		}

	}

}
