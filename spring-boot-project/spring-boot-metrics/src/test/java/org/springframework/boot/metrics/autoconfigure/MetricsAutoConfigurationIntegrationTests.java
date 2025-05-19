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

package org.springframework.boot.metrics.autoconfigure;

import java.util.Arrays;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for metrics auto-configuration.
 *
 * @author Stephane Nicoll
 */
class MetricsAutoConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class))
		.withPropertyValues("management.metrics.use-global-registry=false");

	@Test
	void propertyBasedMeterFilteringIsAutoConfigured() {
		this.contextRunner.withPropertyValues("management.metrics.enable.my.org=false").run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.timer("my.org.timer");
			assertThat(registry.find("my.org.timer").timer()).isNull();
		});
	}

	@Test
	void propertyBasedCommonTagsIsAutoConfigured() {
		this.contextRunner
			.withPropertyValues("management.metrics.tags.region=test", "management.metrics.tags.origin=local")
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.counter("my.counter", "env", "qa");
				assertThat(registry.find("my.counter")
					.tags("env", "qa")
					.tags("region", "test")
					.tags("origin", "local")
					.counter()).isNotNull();
			});
	}

	@Test
	void emptyCompositeIsCreatedWhenNoMeterRegistriesAreAutoConfigured() {
		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry).isInstanceOf(CompositeMeterRegistry.class);
			assertThat(((CompositeMeterRegistry) registry).getRegistries()).isEmpty();
		});
	}

	@Test
	void noCompositeIsCreatedWhenASingleMeterRegistryIsAutoConfigured() {
		this.contextRunner.withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.run((context) -> assertThat(context.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class));
	}

	@Test
	void noCompositeIsCreatedWithMultipleRegistriesAndOneThatIsPrimary() {
		this.contextRunner
			.withBean("meterRegistry1", SimpleMeterRegistry.class, SimpleMeterRegistry::new,
					(definition) -> definition.setPrimary(true))
			.withBean("meterRegistry2", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.run((context) -> assertThat(context.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class));
	}

	@Test
	void compositeCreatedWithMultipleRegistries() {
		this.contextRunner.withBean("meterRegistry1", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.withBean("meterRegistry2", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.run((context) -> {
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry).isInstanceOf(CompositeMeterRegistry.class);
				assertThat(((CompositeMeterRegistry) registry).getRegistries()).hasSize(2);
			});
	}

	@Test
	void autoConfiguredCompositeDoesNotHaveMeterFiltersApplied() {
		this.contextRunner.withBean("meterRegistry1", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.withBean("meterRegistry2", SimpleMeterRegistry.class, SimpleMeterRegistry::new)
			.run((context) -> {
				MeterRegistry composite = context.getBean(MeterRegistry.class);
				assertThat(composite).extracting("filters", InstanceOfAssertFactories.ARRAY).isEmpty();
				assertThat(composite).isInstanceOf(CompositeMeterRegistry.class);
				Set<MeterRegistry> registries = ((CompositeMeterRegistry) composite).getRegistries();
				assertThat(registries).hasSize(2);
				assertThat(registries).allSatisfy(
						(registry) -> assertThat(registry).extracting("filters", InstanceOfAssertFactories.ARRAY)
							.hasSize(1));
			});
	}

	@Test
	void userConfiguredCompositeHasMeterFiltersApplied() {
		this.contextRunner.withUserConfiguration(CompositeMeterRegistryConfiguration.class).run((context) -> {
			MeterRegistry composite = context.getBean(MeterRegistry.class);
			assertThat(composite).extracting("filters", InstanceOfAssertFactories.ARRAY).hasSize(1);
			assertThat(composite).isInstanceOf(CompositeMeterRegistry.class);
			Set<MeterRegistry> registries = ((CompositeMeterRegistry) composite).getRegistries();
			assertThat(registries).hasSize(2);
			assertThat(registries).hasOnlyElementsOfTypes(SimpleMeterRegistry.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CompositeMeterRegistryConfiguration {

		@Bean
		CompositeMeterRegistry compositeMeterRegistry() {
			return new CompositeMeterRegistry(new MockClock(),
					Arrays.asList(new SimpleMeterRegistry(), new SimpleMeterRegistry()));
		}

	}

}
