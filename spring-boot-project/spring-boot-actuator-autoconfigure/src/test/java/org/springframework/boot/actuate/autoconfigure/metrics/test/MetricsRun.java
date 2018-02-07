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

package org.springframework.boot.actuate.autoconfigure.metrics.test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.datadog.DatadogMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia.GangliaMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.graphite.GraphiteMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.jmx.JmxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.statsd.StatsdMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.Assert;

/**
 * Additional metrics configuration and settings that can be applied to a
 * {@link ApplicationContextRunner} when running a metrics test.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
public final class MetricsRun {

	private static final Set<Class<?>> IMPLEMENTATIONS;

	static {
		Set<Class<?>> implementations = new LinkedHashSet<>();
		implementations.add(AtlasMetricsExportAutoConfiguration.class);
		implementations.add(DatadogMetricsExportAutoConfiguration.class);
		implementations.add(GangliaMetricsExportAutoConfiguration.class);
		implementations.add(GraphiteMetricsExportAutoConfiguration.class);
		implementations.add(InfluxMetricsExportAutoConfiguration.class);
		implementations.add(JmxMetricsExportAutoConfiguration.class);
		implementations.add(PrometheusMetricsExportAutoConfiguration.class);
		implementations.add(SimpleMetricsExportAutoConfiguration.class);
		implementations.add(StatsdMetricsExportAutoConfiguration.class);
		IMPLEMENTATIONS = Collections.unmodifiableSet(implementations);
	}

	private MetricsRun() {
	}

	/**
	 * Return a function that configures the run to be limited to the {@code simple}
	 * implementation.
	 * @return the function to apply
	 */
	public static Function<ApplicationContextRunner, ApplicationContextRunner> simple() {
		return limitedTo(SimpleMetricsExportAutoConfiguration.class);
	}

	/**
	 * Return a function that configures the run to be limited to the specified
	 * implementations.
	 * @param implementations the implementations to include
	 * @return the function to apply
	 */
	public static Function<ApplicationContextRunner, ApplicationContextRunner> limitedTo(
			Class<?>... implementations) {
		return (contextRunner) -> apply(contextRunner, implementations);
	}

	private static ApplicationContextRunner apply(ApplicationContextRunner contextRunner,
			Class<?>[] implementations) {
		for (Class<?> implementation : implementations) {
			Assert.state(IMPLEMENTATIONS.contains(implementation),
					"Unknown implementation " + implementation.getName());
		}
		return contextRunner
				.withPropertyValues("management.metrics.use-global-registry=false")
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class))
				.withConfiguration(AutoConfigurations.of(implementations));
	}

}
