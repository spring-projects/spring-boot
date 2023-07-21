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

package org.springframework.boot.actuate.autoconfigure.neo4j;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Neo4jHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Michael J. Simons
 */
class Neo4jHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				Neo4jHealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateHealthIndicator() {
		this.contextRunner.withUserConfiguration(Neo4jConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Neo4jReactiveHealthIndicator.class)
				.doesNotHaveBean(Neo4jHealthIndicator.class));
	}

	@Test
	void runWithoutReactorShouldCreateHealthIndicator() {
		this.contextRunner.withUserConfiguration(Neo4jConfiguration.class)
			.withClassLoader(new FilteredClassLoader(Flux.class))
			.run((context) -> assertThat(context).hasSingleBean(Neo4jHealthIndicator.class)
				.doesNotHaveBean(Neo4jReactiveHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withUserConfiguration(Neo4jConfiguration.class)
			.withPropertyValues("management.health.neo4j.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(Neo4jHealthIndicator.class)
				.doesNotHaveBean(Neo4jReactiveHealthIndicator.class));
	}

	@Test
	void defaultIndicatorCanBeReplaced() {
		this.contextRunner.withUserConfiguration(Neo4jConfiguration.class, CustomIndicatorConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("neo4jHealthIndicator");
				Health health = context.getBean("neo4jHealthIndicator", HealthIndicator.class).health();
				assertThat(health.getDetails()).containsOnly(entry("test", true));
			});
	}

	@Test
	void shouldRequireDriverBean() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Neo4jHealthIndicator.class)
			.doesNotHaveBean(Neo4jReactiveHealthIndicator.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class Neo4jConfiguration {

		@Bean
		Driver driver() {
			return mock(Driver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomIndicatorConfiguration {

		@Bean
		HealthIndicator neo4jHealthIndicator() {
			return new AbstractHealthIndicator() {

				@Override
				protected void doHealthCheck(Health.Builder builder) {
					builder.up().withDetail("test", true);
				}
			};
		}

	}

}
