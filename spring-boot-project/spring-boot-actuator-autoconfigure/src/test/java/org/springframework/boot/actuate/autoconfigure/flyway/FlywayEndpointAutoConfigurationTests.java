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

package org.springframework.boot.actuate.autoconfigure.flyway;

import org.flywaydb.core.Flyway;
import org.junit.Test;

import org.springframework.boot.actuate.flyway.FlywayEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FlywayEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class FlywayEndpointAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(FlywayEndpointAutoConfiguration.class))
			.withUserConfiguration(FlywayConfiguration.class);

	@Test
	public void runShouldHaveEndpointBean() {
		this.contextRunner.run(
				(context) -> assertThat(context).hasSingleBean(FlywayEndpoint.class));
	}

	@Test
	public void runWhenEnabledPropertyIsFalseShouldNotHaveEndpointBean() {
		this.contextRunner.withPropertyValues("management.endpoint.flyway.enabled:false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(FlywayEndpoint.class));
	}

	@Configuration
	static class FlywayConfiguration {

		@Bean
		public Flyway flyway() {
			return mock(Flyway.class);
		}

	}

}
