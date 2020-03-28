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

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraDriverHealthIndicator;
import org.springframework.boot.actuate.cassandra.CassandraDriverReactiveHealthIndicator;
import org.springframework.boot.actuate.cassandra.CassandraHealthIndicator;
import org.springframework.boot.actuate.cassandra.CassandraReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraDriverHealthContributorAutoConfiguration}.
 *
 * @author Alexandre Dutra
 * @since 2.4.0
 */
class CassandraDriverHealthContributorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(CqlSession.class, () -> mock(CqlSession.class)).withConfiguration(AutoConfigurations.of(
					CassandraDriverHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateDriverIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(CassandraDriverHealthIndicator.class)
				.hasBean("cassandraHealthContributor").doesNotHaveBean(CassandraHealthIndicator.class)
				.doesNotHaveBean(CassandraReactiveHealthIndicator.class)
				.doesNotHaveBean(CassandraDriverReactiveHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateDriverIndicator() {
		this.contextRunner.withPropertyValues("management.health.cassandra.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(CassandraDriverHealthIndicator.class)
						.doesNotHaveBean("cassandraHealthContributor"));
	}

	@Test
	void runWhenSpringDataPresentShouldNotCreateDriverIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(CassandraHealthContributorAutoConfiguration.class))
				.withBean(CassandraOperations.class, () -> mock(CassandraOperations.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CassandraDriverHealthIndicator.class)
						.hasSingleBean(CassandraHealthIndicator.class).hasBean("cassandraHealthContributor"));
	}

	@Test
	void runWhenReactorPresentShouldNotCreateDriverIndicator() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(CassandraDriverReactiveHealthContributorAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CassandraDriverHealthIndicator.class)
						.hasSingleBean(CassandraDriverReactiveHealthIndicator.class)
						.hasBean("cassandraHealthContributor"));
	}

	@Test
	void runWhenSpringDataAndReactorPresentShouldNotCreateDriverIndicator() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(CassandraReactiveHealthContributorAutoConfiguration.class))
				.withBean(ReactiveCassandraOperations.class, () -> mock(ReactiveCassandraOperations.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CassandraDriverHealthIndicator.class)
						.hasSingleBean(CassandraReactiveHealthIndicator.class).hasBean("cassandraHealthContributor"));
	}

}
