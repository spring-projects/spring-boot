/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.autoconfigure.health;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cassandra.health.CassandraDriverHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class CassandraHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CassandraHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runWithoutCqlSessionShouldNotCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("cassandraHealthContributor")
			.doesNotHaveBean(CassandraDriverHealthIndicator.class));
	}

	@Test
	void runWithCqlSessionShouldCreateDriverIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.run((context) -> assertThat(context).hasSingleBean(CassandraDriverHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.withPropertyValues("management.health.cassandra.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean("cassandraHealthContributor")
				.doesNotHaveBean(CassandraDriverHealthIndicator.class));
	}

}
