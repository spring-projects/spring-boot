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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Integration tests for {@link MeterRegistryConfigurer}.
 *
 * @author Jon Schneider
 */
public class MeterRegistryConfigurerIntegrationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.limitedTo(AtlasMetricsExportAutoConfiguration.class,
					PrometheusMetricsExportAutoConfiguration.class));

	@Test
	public void binderMetricsAreSearchableFromTheComposite() {
		this.contextRunner.run((context) -> {
			CompositeMeterRegistry composite = context
					.getBean(CompositeMeterRegistry.class);
			composite.get("jvm.memory.used").gauge();
			context.getBeansOfType(MeterRegistry.class)
					.forEach((name, registry) -> registry.get("jvm.memory.used").gauge());
		});
	}

}
