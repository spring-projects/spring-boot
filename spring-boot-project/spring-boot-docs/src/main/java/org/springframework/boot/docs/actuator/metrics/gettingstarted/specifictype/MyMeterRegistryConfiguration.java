/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.actuator.metrics.gettingstarted.specifictype;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.graphite.GraphiteMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyMeterRegistryConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyMeterRegistryConfiguration {

	/**
	 * Configures the naming convention for GraphiteMeterRegistry.
	 * @return the MeterRegistryCustomizer that sets the naming convention for
	 * GraphiteMeterRegistry
	 */
	@Bean
	public MeterRegistryCustomizer<GraphiteMeterRegistry> graphiteMetricsNamingConvention() {
		return (registry) -> registry.config().namingConvention(this::name);
	}

	/**
	 * Returns the name of the meter with the specified parameters.
	 * @param name the name of the meter
	 * @param type the type of the meter
	 * @param baseUnit the base unit of the meter
	 * @return the name of the meter in snake case format
	 */
	private String name(String name, Meter.Type type, String baseUnit) {
		return /**/ NamingConvention.snakeCase.name(name, type, baseUnit);
	}

}
