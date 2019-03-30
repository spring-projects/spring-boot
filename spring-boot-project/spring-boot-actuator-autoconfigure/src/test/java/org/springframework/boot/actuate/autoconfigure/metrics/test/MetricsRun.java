/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.datadog.DatadogMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia.GangliaMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.graphite.GraphiteMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.jmx.JmxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic.NewRelicMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx.SignalFxMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.statsd.StatsdMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
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

	private static final Set<Class<?>> EXPORT_AUTO_CONFIGURATIONS;

	static {
		Set<Class<?>> implementations = new LinkedHashSet<>();
		implementations.add(AtlasMetricsExportAutoConfiguration.class);
		implementations.add(DatadogMetricsExportAutoConfiguration.class);
		implementations.add(GangliaMetricsExportAutoConfiguration.class);
		implementations.add(GraphiteMetricsExportAutoConfiguration.class);
		implementations.add(InfluxMetricsExportAutoConfiguration.class);
		implementations.add(JmxMetricsExportAutoConfiguration.class);
		implementations.add(NewRelicMetricsExportAutoConfiguration.class);
		implementations.add(PrometheusMetricsExportAutoConfiguration.class);
		implementations.add(SimpleMetricsExportAutoConfiguration.class);
		implementations.add(SignalFxMetricsExportAutoConfiguration.class);
		implementations.add(StatsdMetricsExportAutoConfiguration.class);
		EXPORT_AUTO_CONFIGURATIONS = Collections.unmodifiableSet(implementations);
	}

	private static final AutoConfigurations AUTO_CONFIGURATIONS = AutoConfigurations.of(
			MetricsAutoConfiguration.class,
			CompositeMeterRegistryAutoConfiguration.class);

	private MetricsRun() {
	}

	/**
	 * Return a function that configures the run to be limited to the {@code simple}
	 * implementation.
	 * @return the function to apply
	 */
	public static <T extends AbstractApplicationContextRunner<?, ?, ?>> Function<T, T> simple() {
		return limitedTo(SimpleMetricsExportAutoConfiguration.class);
	}

	/**
	 * Return a function that configures the run to be limited to the specified
	 * implementations.
	 * @param exportAutoConfigurations the export auto-configurations to include
	 * @return the function to apply
	 */
	public static <T extends AbstractApplicationContextRunner<?, ?, ?>> Function<T, T> limitedTo(
			Class<?>... exportAutoConfigurations) {
		return (contextRunner) -> apply(contextRunner, exportAutoConfigurations);
	}

	@SuppressWarnings("unchecked")
	private static <T extends AbstractApplicationContextRunner<?, ?, ?>> T apply(
			T contextRunner, Class<?>[] exportAutoConfigurations) {
		for (Class<?> configuration : exportAutoConfigurations) {
			Assert.state(EXPORT_AUTO_CONFIGURATIONS.contains(configuration),
					() -> "Unknown export auto-configuration " + configuration.getName());
		}
		return (T) contextRunner
				.withPropertyValues("management.metrics.use-global-registry=false")
				.withConfiguration(AUTO_CONFIGURATIONS)
				.withConfiguration(AutoConfigurations.of(exportAutoConfigurations));
	}

}
