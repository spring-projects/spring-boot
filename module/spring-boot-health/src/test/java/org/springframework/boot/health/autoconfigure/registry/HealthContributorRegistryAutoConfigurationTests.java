/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.autoconfigure.registry;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthContributorRegistryAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class HealthContributorRegistryAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthContributorRegistryAutoConfiguration.class));

	@Test
	void runCreatesHealthContributorRegistriesContainingHealthBeans() {
		this.contextRunner.run((context) -> {
			HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
			Object[] names = registry.stream().map(HealthContributors.Entry::name).toArray();
			assertThat(names).containsExactlyInAnyOrder("simple", "additional", "ping");
			ReactiveHealthContributorRegistry reactiveRegistry = context
				.getBean(ReactiveHealthContributorRegistry.class);
			Object[] reactiveNames = reactiveRegistry.stream().map(ReactiveHealthContributors.Entry::name).toArray();
			assertThat(reactiveNames).containsExactlyInAnyOrder("reactive");
		});
	}

	@Test
	void runWhenNoReactorCreatesHealthContributorRegistryContainingHealthBeans() {
		ClassLoader classLoader = new FilteredClassLoader(Mono.class, Flux.class);
		this.contextRunner.withClassLoader(classLoader).run((context) -> {
			HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
			Object[] names = registry.stream().map(HealthContributors.Entry::name).toArray();
			assertThat(names).containsExactlyInAnyOrder("simple", "additional", "ping");
		});
	}

	@Test
	void runWhenHasHealthContributorRegistryBeanDoesNotCreateAdditionalRegistry() {
		this.contextRunner.withUserConfiguration(HealthContributorRegistryConfiguration.class).run((context) -> {
			HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
			Object[] names = registry.stream().map(HealthContributors.Entry::name).toArray();
			assertThat(names).isEmpty();
		});
	}

	@Test
	void runWhenHasReactiveHealthContributorRegistryBeanDoesNotCreateAdditionalReactiveHealthContributorRegistry() {
		this.contextRunner.withUserConfiguration(ReactiveHealthContributorRegistryConfiguration.class)
			.run((context) -> {
				ReactiveHealthContributorRegistry registry = context.getBean(ReactiveHealthContributorRegistry.class);
				Object[] names = registry.stream().map(ReactiveHealthContributors.Entry::name).toArray();
				assertThat(names).isEmpty();
			});
	}

	@Test
	void runWithIndicatorsInParentContextFindsIndicators() {
		new ApplicationContextRunner().withUserConfiguration(HealthIndicatorsConfiguration.class)
			.run((parent) -> new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
						HealthContributorRegistryAutoConfiguration.class))
				.withParent(parent)
				.run((context) -> {
					HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
					Object[] names = registry.stream().map(HealthContributors.Entry::name).toArray();
					assertThat(names).containsExactlyInAnyOrder("simple", "additional", "ping");
					ReactiveHealthContributorRegistry reactiveRegistry = context
						.getBean(ReactiveHealthContributorRegistry.class);
					Object[] reactiveNames = reactiveRegistry.stream()
						.map(ReactiveHealthContributors.Entry::name)
						.toArray();
					assertThat(reactiveNames).containsExactlyInAnyOrder("reactive");
				}));
	}

	@Configuration(proxyBeanMethods = false)
	static class HealthIndicatorsConfiguration {

		@Bean
		HealthIndicator simpleHealthIndicator() {
			return () -> Health.up().withDetail("counter", 42).build();
		}

		@Bean
		HealthIndicator additionalHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		ReactiveHealthIndicator reactiveHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HealthContributorRegistryConfiguration {

		@Bean
		HealthContributorRegistry healthContributorRegistry() {
			return new DefaultHealthContributorRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveHealthContributorRegistryConfiguration {

		@Bean
		ReactiveHealthContributorRegistry reactiveHealthContributorRegistry() {
			return new DefaultReactiveHealthContributorRegistry();
		}

	}

}
