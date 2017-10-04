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

package org.springframework.boot.actuate.autoconfigure.metrics.export.simple;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleExportConfiguration}.
 *
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
public class SimpleExportConfigurationTests {

	@Test
	public void simpleMeterRegistryIsInTheCompositeWhenNoOtherRegistryIs() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.metrics.atlas.enabled=false",
						"spring.metrics.datadog.enabled=false",
						"spring.metrics.ganglia.enabled=false",
						"spring.metrics.graphite.enabled=false",
						"spring.metrics.influx.enabled=false",
						"spring.metrics.jmx.enabled=false",
						"spring.metrics.prometheus.enabled=false")
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class))
				.run((context) -> {
					CompositeMeterRegistry meterRegistry = context
							.getBean(CompositeMeterRegistry.class);
					assertThat(meterRegistry.getRegistries()).hasSize(1);
					assertThat(meterRegistry.getRegistries())
							.hasOnlyElementsOfType(SimpleMeterRegistry.class);
				});
	}

}
