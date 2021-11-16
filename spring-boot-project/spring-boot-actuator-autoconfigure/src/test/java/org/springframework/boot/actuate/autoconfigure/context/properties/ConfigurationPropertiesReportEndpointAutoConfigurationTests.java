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

package org.springframework.boot.actuate.autoconfigure.context.properties;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ApplicationConfigurationProperties;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpointWebExtension;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertiesReportEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ConfigurationPropertiesReportEndpointAutoConfiguration.class));

	@Test
	void runShouldHaveEndpointBean() {
		this.contextRunner.withUserConfiguration(Config.class)
				.withPropertyValues("management.endpoints.web.exposure.include=configprops")
				.run(validateTestProperties("******", "654321"));
	}

	@Test
	void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.configprops.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(ConfigurationPropertiesReportEndpoint.class));
	}

	@Test
	void keysToSanitizeCanBeConfiguredViaTheEnvironment() {
		this.contextRunner.withUserConfiguration(Config.class)
				.withPropertyValues("management.endpoint.configprops.keys-to-sanitize: .*pass.*, property")
				.withPropertyValues("management.endpoints.web.exposure.include=configprops")
				.run(validateTestProperties("******", "******"));
	}

	@Test
	void additionalKeysToSanitizeCanBeConfiguredViaTheEnvironment() {
		this.contextRunner.withUserConfiguration(Config.class)
				.withPropertyValues("management.endpoint.configprops.additional-keys-to-sanitize: property")
				.withPropertyValues("management.endpoints.web.exposure.include=configprops")
				.run(validateTestProperties("******", "******"));
	}

	@Test
	void customSanitizingFunctionShouldBeApplied() {
		this.contextRunner.withUserConfiguration(Config.class, SanitizingFunctionConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=configprops",
						"test.my-test-property=abc")
				.run(validateTestProperties("******", "$$$"));
	}

	@Test
	void runWhenNotExposedShouldNotHaveEndpointBean() {
		this.contextRunner
				.run((context) -> assertThat(context).doesNotHaveBean(ConfigurationPropertiesReportEndpoint.class));
	}

	private ContextConsumer<AssertableApplicationContext> validateTestProperties(String dbPassword,
			String myTestProperty) {
		return (context) -> {
			assertThat(context).hasSingleBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ApplicationConfigurationProperties properties = endpoint.configurationProperties();
			Map<String, Object> nestedProperties = properties.getContexts().get(context.getId()).getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties).isNotNull();
			assertThat(nestedProperties.get("dbPassword")).isEqualTo(dbPassword);
			assertThat(nestedProperties.get("myTestProperty")).isEqualTo(myTestProperty);
		};
	}

	@Test
	void runWhenOnlyExposedOverJmxShouldHaveEndpointBeanWithoutWebExtension() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=info", "spring.jmx.enabled=true",
						"management.endpoints.jmx.exposure.include=configprops")
				.run((context) -> assertThat(context).hasSingleBean(ConfigurationPropertiesReportEndpoint.class)
						.doesNotHaveBean(ConfigurationPropertiesReportEndpointWebExtension.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class Config {

		@Bean
		TestProperties testProperties() {
			return new TestProperties();
		}

	}

	@ConfigurationProperties("test")
	public static class TestProperties {

		private String dbPassword = "123456";

		private String myTestProperty = "654321";

		public String getDbPassword() {
			return this.dbPassword;
		}

		public void setDbPassword(String dbPassword) {
			this.dbPassword = dbPassword;
		}

		public String getMyTestProperty() {
			return this.myTestProperty;
		}

		public void setMyTestProperty(String myTestProperty) {
			this.myTestProperty = myTestProperty;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SanitizingFunctionConfiguration {

		@Bean
		SanitizingFunction testSanitizingFunction() {
			return (data) -> {
				if (data.getKey().contains("my")) {
					return data.withValue("$$$");
				}
				return data;
			};
		}

	}

}
