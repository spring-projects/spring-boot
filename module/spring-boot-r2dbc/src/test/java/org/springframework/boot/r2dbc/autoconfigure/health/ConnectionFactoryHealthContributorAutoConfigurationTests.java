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

package org.springframework.boot.r2dbc.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.health.ConnectionFactoryHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConnectionFactoryHealthContributorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ConnectionFactoryHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ConnectionFactoryHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(ConnectionFactoryHealthIndicator.class));
	}

	@Test
	void runWithNoConnectionFactoryShouldNotCreateIndicator() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactoryHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.withPropertyValues("management.health.r2dbc.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ConnectionFactoryHealthIndicator.class));
	}

}
