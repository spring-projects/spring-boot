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

package org.springframework.boot.actuate.autoconfigure.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.cassandra.CassandraDriverHealthIndicator;
import org.springframework.boot.actuate.cassandra.CassandraDriverReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraReactiveHealthContributorAutoConfiguration}.
 *
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 */
class CassandraReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CassandraReactiveHealthContributorAutoConfiguration.class,
				CassandraHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runWithoutCqlSessionOrReactiveCassandraOperationsShouldNotCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("cassandraHealthContributor")
			.doesNotHaveBean(CassandraDriverReactiveHealthIndicator.class));
	}

	@Test
	void runWithCqlSessionOnlyShouldCreateDriverIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.run((context) -> assertThat(context).hasBean("cassandraHealthContributor")
				.hasSingleBean(CassandraDriverReactiveHealthIndicator.class));
	}

	@Test
	void runWithCqlSessionAndReactiveCassandraOperationsShouldCreateDriverIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.withBean(ReactiveCassandraOperations.class, () -> mock(ReactiveCassandraOperations.class))
			.withBean(CassandraOperations.class, () -> mock(CassandraOperations.class))
			.run((context) -> assertThat(context).hasBean("cassandraHealthContributor")
				.hasSingleBean(CassandraDriverReactiveHealthIndicator.class)
				.doesNotHaveBean(CassandraDriverHealthIndicator.class));
	}

	@Test
	void runWithCqlSessionAndSpringDataAbsentShouldCreateDriverIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.withClassLoader(new FilteredClassLoader("org.springframework.data"))
			.run((context) -> assertThat(context).hasBean("cassandraHealthContributor")
				.hasSingleBean(CassandraDriverReactiveHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.withBean(ReactiveCassandraOperations.class, () -> mock(ReactiveCassandraOperations.class))
			.withPropertyValues("management.health.cassandra.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean("cassandraHealthContributor"));
	}

}
