/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class CompositeMeterRegistryConfigurationTests {
	/**
	 * The simple registry is off by default UNLESS there is no other registry implementation on
	 * the classpath, in which case it is on.
	 */
	@Test
	public void simpleWithNoCompositeCreated() {
		MetricsContextBuilder.contextRunner("simple").run(context ->
				assertThat(context.getBean(MeterRegistry.class))
						.isInstanceOf(SimpleMeterRegistry.class));
	}

	/**
	 * An empty composite is created in the absence of any other registry implementation.
	 * This effectively no-ops instrumentation code throughout the application.
	 */
	@Test
	public void emptyCompositeCreated() {
		MetricsContextBuilder.contextRunner().run(context ->
				assertThat(context.getBean(MeterRegistry.class))
						.isInstanceOf(CompositeMeterRegistry.class)
						.matches(r -> ((CompositeMeterRegistry) r).getRegistries().isEmpty()));
	}

	@Test
	public void noCompositeCreatedWhenSingleImplementationIsEnabled() {
		MetricsContextBuilder.contextRunner("graphite").run(context ->
				assertThat(context.getBean(MeterRegistry.class))
						.isInstanceOf(GraphiteMeterRegistry.class));
	}

	@Test
	public void noCompositeCreatedWhenMultipleRegistriesButOneMarkedAsPrimary() {
		MetricsContextBuilder.contextRunner("graphite", "jmx")
				.withUserConfiguration(PrimarySimpleMeterRegistryConfiguration.class)
				.run(context -> assertThat(context.getBean(MeterRegistry.class))
						.isInstanceOf(SimpleMeterRegistry.class));
	}

	@Test
	public void compositeCreatedWhenMultipleImplementationsAreEnabled() {
		MetricsContextBuilder.contextRunner("graphite", "jmx").run(context -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry).isInstanceOf(CompositeMeterRegistry.class);

			assertThat(((CompositeMeterRegistry) registry).getRegistries())
					.hasAtLeastOneElementOfType(GraphiteMeterRegistry.class)
					.hasAtLeastOneElementOfType(JmxMeterRegistry.class);
		});
	}

	@Configuration
	static class PrimarySimpleMeterRegistryConfiguration {
		@Primary
		@Bean
		MeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
		}
	}
}
