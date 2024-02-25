/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.jmx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to JMX.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(JmxMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("jmx")
@EnableConfigurationProperties(JmxProperties.class)
public class JmxMetricsExportAutoConfiguration {

	/**
	 * Creates a JmxConfig bean if no other bean of type JmxConfig is present in the
	 * application context. Uses the provided JmxProperties to create a
	 * JmxPropertiesConfigAdapter.
	 * @param jmxProperties the JmxProperties object used to configure the JmxConfig bean
	 * @return a JmxConfig bean created using the JmxPropertiesConfigAdapter
	 */
	@Bean
	@ConditionalOnMissingBean
	public JmxConfig jmxConfig(JmxProperties jmxProperties) {
		return new JmxPropertiesConfigAdapter(jmxProperties);
	}

	/**
	 * Creates a JmxMeterRegistry bean if there is no existing bean of the same type.
	 * @param jmxConfig the JmxConfig object used for configuring the JmxMeterRegistry
	 * @param clock the Clock object used for measuring time
	 * @return a new instance of JmxMeterRegistry
	 */
	@Bean
	@ConditionalOnMissingBean
	public JmxMeterRegistry jmxMeterRegistry(JmxConfig jmxConfig, Clock clock) {
		return new JmxMeterRegistry(jmxConfig, clock);
	}

}
