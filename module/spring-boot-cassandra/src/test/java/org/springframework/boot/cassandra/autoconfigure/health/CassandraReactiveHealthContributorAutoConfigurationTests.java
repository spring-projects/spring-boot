/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.autoconfigure.health;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cassandra.health.CassandraDriverReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
	void runWithoutCqlSessionShouldNotCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("cassandraHealthContributor")
			.doesNotHaveBean(CassandraDriverReactiveHealthIndicator.class));
	}

	@Test
	void runWithCqlSessionShouldCreateIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.run((context) -> assertThat(context).hasBean("cassandraHealthContributor")
				.hasSingleBean(CassandraDriverReactiveHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withBean(CqlSession.class, () -> mock(CqlSession.class))
			.withPropertyValues("management.health.cassandra.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean("cassandraHealthContributor"));
	}

}
