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

package org.springframework.boot.actuate.autoconfigure.data.elasticsearch;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticsearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.data.elasticsearch.ElasticsearchReactiveHealthIndicator;
import org.springframework.boot.actuate.elasticsearch.ElasticsearchRestClientHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ReactiveElasticsearchClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchReactiveHealthContributorAutoConfiguration}.
 *
 * @author Aleksander Lech
 */
class ElasticsearchReactiveHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ElasticsearchDataAutoConfiguration.class,
				ReactiveElasticsearchClientAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class,
				ElasticsearchReactiveHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ElasticsearchReactiveHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor"));
	}

	@Test
	void runWithRegularIndicatorShouldOnlyCreateReactiveIndicator() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestHealthContributorAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(ElasticsearchReactiveHealthIndicator.class)
				.hasBean("elasticsearchHealthContributor")
				.doesNotHaveBean(ElasticsearchRestClientHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.elasticsearch.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchReactiveHealthIndicator.class)
				.doesNotHaveBean("elasticsearchHealthContributor"));
	}

}
