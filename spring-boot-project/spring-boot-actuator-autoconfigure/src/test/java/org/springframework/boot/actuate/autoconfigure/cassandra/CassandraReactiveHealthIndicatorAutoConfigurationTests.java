/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cassandra;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraHealthIndicator;
import org.springframework.boot.actuate.cassandra.CassandraReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraReactiveHealthIndicatorAutoConfiguration}.
 *
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 */
class CassandraReactiveHealthIndicatorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(ReactiveCassandraOperations.class, () -> mock(ReactiveCassandraOperations.class))
			.withConfiguration(AutoConfigurations.of(CassandraReactiveHealthIndicatorAutoConfiguration.class,
					HealthIndicatorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(CassandraReactiveHealthIndicator.class)
				.doesNotHaveBean(CassandraHealthIndicator.class).doesNotHaveBean(ApplicationHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.cassandra.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(CassandraReactiveHealthIndicator.class)
						.hasSingleBean(ApplicationHealthIndicator.class));
	}

}
