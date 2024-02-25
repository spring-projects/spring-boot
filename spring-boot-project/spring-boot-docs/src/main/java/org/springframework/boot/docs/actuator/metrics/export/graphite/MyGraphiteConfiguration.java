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

package org.springframework.boot.docs.actuator.metrics.export.graphite;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyGraphiteConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyGraphiteConfiguration {

	/**
	 * Creates a new instance of GraphiteMeterRegistry using the provided GraphiteConfig
	 * and Clock.
	 * @param config the GraphiteConfig to use for configuring the GraphiteMeterRegistry
	 * @param clock the Clock to use for measuring time
	 * @return a new instance of GraphiteMeterRegistry
	 */
	@Bean
	public GraphiteMeterRegistry graphiteMeterRegistry(GraphiteConfig config, Clock clock) {
		return new GraphiteMeterRegistry(config, clock, this::toHierarchicalName);
	}

	/**
	 * Converts a Meter.Id object to a hierarchical name using the specified
	 * NamingConvention.
	 * @param id The Meter.Id object to convert.
	 * @param convention The NamingConvention to use for the conversion.
	 * @return The hierarchical name generated from the Meter.Id object.
	 */
	private String toHierarchicalName(Meter.Id id, NamingConvention convention) {
		return /**/ HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, convention);
	}

}
