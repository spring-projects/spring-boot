/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure.health;

import com.couchbase.client.java.Cluster;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.couchbase.health.CouchbaseHealthIndicator;
import org.springframework.boot.couchbase.health.CouchbaseReactiveHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CouchbaseReactiveHealthContributorAutoConfiguration}.
 *
 * @author Mikalai Lushchytski
 */
class CouchbaseReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(Cluster.class, () -> mock(Cluster.class))
		.withConfiguration(AutoConfigurations.of(CouchbaseReactiveHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(CouchbaseReactiveHealthIndicator.class)
			.hasBean("couchbaseHealthContributor"));
	}

	@Test
	void runWithRegularIndicatorShouldOnlyCreateReactiveIndicator() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(CouchbaseHealthContributorAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(CouchbaseReactiveHealthIndicator.class)
				.hasBean("couchbaseHealthContributor")
				.doesNotHaveBean(CouchbaseHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.couchbase.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(CouchbaseReactiveHealthIndicator.class)
				.doesNotHaveBean("couchbaseHealthContributor"));
	}

}
