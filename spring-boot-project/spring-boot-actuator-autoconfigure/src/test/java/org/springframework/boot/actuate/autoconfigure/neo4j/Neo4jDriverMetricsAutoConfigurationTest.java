/*
 * Copyright 2012-2020 the original author or authors.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.neo4j.Neo4jDriverMetrics;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.neo4j.driver.Driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.actuate.autoconfigure.neo4j.Neo4jDriverMocks.mockDriverWithMetrics;
import static org.springframework.boot.actuate.autoconfigure.neo4j.Neo4jDriverMocks.mockDriverWithoutMetrics;

/**
 * @author Michael J. Simons
 */
class Neo4jDriverMetricsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(MetricsAutoConfiguration.class, Neo4jDriverMetricsAutoConfiguration.class));

	private final ContextConsumer<AssertableApplicationContext> assertNoInteractionsWithRegistry = ctx -> {

		if (ctx.getBeansOfType(MeterRegistry.class).isEmpty()) {
			return;
		}

		MeterRegistry mockedRegistry = ctx.getBean(MeterRegistry.class);
		verify(mockedRegistry).config();
		verifyNoMoreInteractions(mockedRegistry);
	};

	@Nested
	class NoMatches {

		@Test
		void shouldRequireAllNeededClasses() {
			contextRunner.withUserConfiguration(WithMeterRegistry.class)
					.withClassLoader(new FilteredClassLoader(Driver.class)).run(assertNoInteractionsWithRegistry);

			contextRunner.withUserConfiguration(WithDriverWithMetrics.class)
					.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
					.run(assertNoInteractionsWithRegistry);
		}

		@Test
		void shouldRequireAllNeededBeans() {
			contextRunner.withUserConfiguration(WithDriverWithMetrics.class).run(assertNoInteractionsWithRegistry);

			contextRunner.withUserConfiguration(WithMeterRegistry.class).run(assertNoInteractionsWithRegistry);
		}

		@Test
		void shouldRequireDriverWithMetrics() {
			contextRunner.withUserConfiguration(WithDriverWithoutMetrics.class, WithMeterRegistry.class)
					.run(assertNoInteractionsWithRegistry);

		}

	}

	@Nested
	class Matches {

		@Test
		void shouldRequireDriverWithMetrics() {
			contextRunner.withUserConfiguration(WithDriverWithMetrics.class, WithMeterRegistry.class).run(ctx -> {

				// Wait a bit to let the completable future of the test that mocks
				// connectiviy complete.
				Thread.sleep(500L);

				MeterRegistry meterRegistry = ctx.getBean(MeterRegistry.class);
				assertThat(meterRegistry.getMeters()).extracting(m -> m.getId().getName())
						.filteredOn(s -> s.startsWith(Neo4jDriverMetrics.PREFIX)).isNotEmpty();
			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithDriverWithMetrics {

		@Bean
		Driver driver() {
			return mockDriverWithMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithDriverWithoutMetrics {

		@Bean
		Driver driver() {
			return mockDriverWithoutMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WithMeterRegistry {

		@Bean
		MeterRegistry meterRegistry() {

			MeterRegistry meterRegistry = spy(SimpleMeterRegistry.class);
			return meterRegistry;
		}

	}

}
