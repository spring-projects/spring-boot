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

package org.springframework.boot.actuate.autoconfigure.elasticsearch;

import io.searchbox.client.JestClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.elasticsearch.ElasticsearchHealthIndicator;
import org.springframework.boot.actuate.elasticsearch.ElasticsearchJestHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticSearchClientHealthContributorAutoConfiguration} and
 * {@link ElasticSearchJestHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 */
@Deprecated
class ElasticsearchHealthContributorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(ElasticsearchAutoConfiguration.class, ElasticSearchClientHealthContributorAutoConfiguration.class,
					ElasticSearchJestHealthContributorAutoConfiguration.class,
					HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.withPropertyValues("spring.data.elasticsearch.cluster-nodes:localhost:0")
				.withSystemProperties("es.set.netty.runtime.available.processors=false")
				.run((context) -> assertThat(context).hasSingleBean(ElasticsearchHealthIndicator.class)
						.doesNotHaveBean(ElasticsearchJestHealthIndicator.class));
	}

	@Test
	void runWhenUsingJestClientShouldCreateIndicator() {
		this.contextRunner.withBean(JestClient.class, () -> mock(JestClient.class))
				.withSystemProperties("es.set.netty.runtime.available.processors=false")
				.run((context) -> assertThat(context).hasSingleBean(ElasticsearchJestHealthIndicator.class)
						.doesNotHaveBean(ElasticsearchHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.elasticsearch.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(ElasticsearchHealthIndicator.class)
						.doesNotHaveBean(ElasticsearchJestHealthIndicator.class));
	}

}
