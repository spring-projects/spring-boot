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

import com.netflix.spectator.atlas.AtlasConfig;
import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.metrics.autoconfigure.jvm.JvmMetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for applying {@link MeterRegistryCustomizer} beans.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 */
class MeterRegistryCustomizerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(AtlasMeterRegistry.class, () -> new AtlasMeterRegistry(new AtlasConfig() {

			@Override
			public String get(String k) {
				return null;
			}

		}))
		.withBean(PrometheusMeterRegistry.class, () -> new PrometheusMeterRegistry(new PrometheusConfig() {

			@Override
			public String get(String key) {
				return null;
			}

		}))
		.withPropertyValues("management.metrics.use-global-registry=false")
		.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
				CompositeMeterRegistryAutoConfiguration.class, JvmMetricsAutoConfiguration.class));

	@Test
	void commonTagsAreAppliedToAutoConfiguredBinders() {
		this.contextRunner.withUserConfiguration(MeterRegistryCustomizerConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("jvm.memory.used").tags("region", "us-east-1").gauge();
		});
	}

	@Test
	void commonTagsAreAppliedBeforeRegistryIsInjectableElsewhere() {
		this.contextRunner.withUserConfiguration(MeterRegistryCustomizerConfiguration.class).run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("my.thing").tags("region", "us-east-1").gauge();
		});
	}

	@Test
	void customizersCanBeAppliedToSpecificRegistryTypes() {
		this.contextRunner.withUserConfiguration(MeterRegistryCustomizerConfiguration.class).run((context) -> {
			MeterRegistry prometheus = context.getBean(PrometheusMeterRegistry.class);
			prometheus.get("jvm.memory.used").tags("job", "myjob").gauge();
			MeterRegistry atlas = context.getBean(AtlasMeterRegistry.class);
			assertThat(atlas.find("jvm.memory.used").tags("job", "myjob").gauge()).isNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MeterRegistryCustomizerConfiguration {

		@Bean
		MeterRegistryCustomizer<MeterRegistry> commonTags() {
			return (registry) -> registry.config().commonTags("region", "us-east-1");
		}

		@Bean
		MeterRegistryCustomizer<PrometheusMeterRegistry> prometheusOnlyCommonTags() {
			return (registry) -> registry.config().commonTags("job", "myjob");
		}

		@Bean
		MyThing myThing(MeterRegistry registry) {
			registry.gauge("my.thing", 0);
			return new MyThing();
		}

		class MyThing {

		}

	}

}
