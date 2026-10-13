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
 * Tests for {@link CouchbaseHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class CouchbaseHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(Cluster.class, () -> mock(Cluster.class))
		.withConfiguration(AutoConfigurations.of(CouchbaseHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(CouchbaseHealthIndicator.class)
			.doesNotHaveBean(CouchbaseReactiveHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.couchbase.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(CouchbaseHealthIndicator.class));
	}

}
