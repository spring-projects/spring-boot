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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.datadog.DatadogMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatadogExportConfiguration}.
 *
 * @author Nikolay Rybak
 */
@RunWith(SpringRunner.class)
public class DatadogExportConfigurationTests {

	/**
	 * Validated that {@link DatadogMeterRegistry} can be started by only specifying Datadog API key.
	 */
	@Test
	public void datadogMeterRegistryIsConfiguredWithApiKeyOnly() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.metrics.export.atlas.enabled=false",
						"spring.metrics.export.datadog.enabled=true",
						"spring.metrics.export.ganglia.enabled=false",
						"spring.metrics.export.graphite.enabled=false",
						"spring.metrics.export.influx.enabled=false",
						"spring.metrics.export.jmx.enabled=false",
						"spring.metrics.export.prometheus.enabled=false",
						"spring.metrics.export.statsd.enabled=false",
						"spring.metrics.export.datadog.api-key=APIKEY")
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class))
				.run((context) -> {
					CompositeMeterRegistry meterRegistry = context
							.getBean(CompositeMeterRegistry.class);
					assertThat(meterRegistry.getRegistries()).hasSize(1);
					assertThat(meterRegistry.getRegistries())
							.hasOnlyElementsOfType(DatadogMeterRegistry.class);
				});
	}

}
