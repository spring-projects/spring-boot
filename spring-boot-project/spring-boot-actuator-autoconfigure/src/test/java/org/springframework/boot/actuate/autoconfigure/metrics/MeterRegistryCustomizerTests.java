/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
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
public class MeterRegistryCustomizerTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.limitedTo(AtlasMetricsExportAutoConfiguration.class,
					PrometheusMetricsExportAutoConfiguration.class));

	@Test
	public void commonTagsAreAppliedToAutoConfiguredBinders() {
		this.contextRunner
				.withUserConfiguration(MeterRegistryCustomizerConfiguration.class)
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("jvm.memory.used").tags("region", "us-east-1").gauge();
				});
	}

	@Test
	public void commonTagsAreAppliedBeforeRegistryIsInjectableElsewhere() {
		this.contextRunner
				.withUserConfiguration(MeterRegistryCustomizerConfiguration.class)
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("my.thing").tags("region", "us-east-1").gauge();
				});
	}

	@Test
	public void customizersCanBeAppliedToSpecificRegistryTypes() {
		this.contextRunner
				.withUserConfiguration(MeterRegistryCustomizerConfiguration.class)
				.run((context) -> {
					MeterRegistry prometheus = context
							.getBean(PrometheusMeterRegistry.class);
					prometheus.get("jvm.memory.used").tags("job", "myjob").gauge();
					MeterRegistry atlas = context.getBean(AtlasMeterRegistry.class);
					assertThat(atlas.find("jvm.memory.used").tags("job", "myjob").gauge())
							.isNull();
				});
	}

	@Configuration
	static class MeterRegistryCustomizerConfiguration {

		@Bean
		public MeterRegistryCustomizer<MeterRegistry> commonTags() {
			return (registry) -> registry.config().commonTags("region", "us-east-1");
		}

		@Bean
		public MeterRegistryCustomizer<PrometheusMeterRegistry> prometheusOnlyCommonTags() {
			return (registry) -> registry.config().commonTags("job", "myjob");
		}

		@Bean
		public MyThing myThing(MeterRegistry registry) {
			registry.gauge("my.thing", 0);
			return new MyThing();
		}

		class MyThing {

		}

	}

}
