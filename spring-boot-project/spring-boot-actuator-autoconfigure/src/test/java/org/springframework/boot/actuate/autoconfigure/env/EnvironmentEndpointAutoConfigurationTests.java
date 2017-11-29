/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.env;

import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertyValueDescriptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class EnvironmentEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(EnvironmentEndpointAutoConfiguration.class));

	@Test
	public void runShouldHaveEndpointBean() {
		this.contextRunner.withSystemProperties("dbPassword=123456", "apiKey=123456")
				.run(validateSystemProperties("******", "******"));
	}

	@Test
	public void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean()
			throws Exception {
		this.contextRunner.withPropertyValues("management.endpoint.env.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(EnvironmentEndpoint.class));
	}

	@Test
	public void keysToSanitizeCanBeConfiguredViaTheEnvironment() throws Exception {
		this.contextRunner.withSystemProperties("dbPassword=123456", "apiKey=123456")
				.withPropertyValues("management.endpoint.env.keys-to-sanitize=.*pass.*")
				.run(validateSystemProperties("******", "123456"));
	}

	private ContextConsumer<AssertableApplicationContext> validateSystemProperties(
			String dbPassword, String apiKey) {
		return (context) -> {
			assertThat(context).hasSingleBean(EnvironmentEndpoint.class);
			EnvironmentEndpoint endpoint = context.getBean(EnvironmentEndpoint.class);
			EnvironmentDescriptor env = endpoint.environment(null);
			Map<String, PropertyValueDescriptor> systemProperties = getSource(
					"systemProperties", env).getProperties();
			assertThat(systemProperties.get("dbPassword").getValue())
					.isEqualTo(dbPassword);
			assertThat(systemProperties.get("apiKey").getValue()).isEqualTo(apiKey);
		};
	}

	private PropertySourceDescriptor getSource(String name,
			EnvironmentDescriptor descriptor) {
		return descriptor.getPropertySources().stream()
				.filter((source) -> name.equals(source.getName())).findFirst().get();
	}

}
