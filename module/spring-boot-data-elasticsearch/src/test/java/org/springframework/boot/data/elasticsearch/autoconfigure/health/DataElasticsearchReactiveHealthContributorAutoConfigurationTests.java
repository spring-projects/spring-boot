/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.autoconfigure.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.boot.data.elasticsearch.health.DataElasticsearchReactiveHealthIndicator;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.health.ElasticsearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.elasticsearch.health.ElasticsearchRestClientHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataElasticsearchReactiveHealthContributorAutoConfiguration}.
 *
 * @author Aleksander Lech
 */
class DataElasticsearchReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataElasticsearchAutoConfiguration.class,
				ElasticsearchClientAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class,
				DataElasticsearchReactiveHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(DataElasticsearchReactiveHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor"));
	}

	@Test
	void runWithRegularIndicatorShouldOnlyCreateReactiveIndicator() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestHealthContributorAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(DataElasticsearchReactiveHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor")
				.doesNotHaveBean(ElasticsearchRestClientHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.elasticsearch.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(DataElasticsearchReactiveHealthIndicator.class)
				.doesNotHaveBean("elasticsearchHealthContributor"));
	}

}
