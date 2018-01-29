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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Additional metrics configuration and settings that can be applied to a
 * {@link ApplicationContextRunner} when running a metrics test.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
public final class MetricsRun {

	private static final Set<String> IMPLEMENTATIONS = Collections
			.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("atlas", "datadog",
					"ganglia", "graphite", "influx", "jmx", "prometheus", "statsd",
					"newrelic", "signalfx", "wavefront", "simple")));

	private MetricsRun() {
	}

	/**
	 * Return a function that configures the run to be limited to the {@code simple}
	 * implementation.
	 * @return the function to apply
	 */
	public static Function<ApplicationContextRunner, ApplicationContextRunner> simple() {
		return limitedTo("simple");
	}

	/**
	 * Return a function that configures the run to be limited to the specified
	 * implementations.
	 * @param implementations the implementations to include
	 * @return the function to apply
	 */
	public static Function<ApplicationContextRunner, ApplicationContextRunner> limitedTo(
			String... implementations) {
		return (contextRunner) -> apply(contextRunner, implementations);
	}

	private static ApplicationContextRunner apply(ApplicationContextRunner contextRunner,
			String[] implementations) {
		return contextRunner.withPropertyValues(getPropertyValues(implementations))
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class));
	}

	private static String[] getPropertyValues(String[] implementations) {
		List<String> propertyValues = new ArrayList<>();
		propertyValues.add("management.metrics.use-global-registry=false");
		List<String> keep = Arrays.asList(implementations);
		IMPLEMENTATIONS.stream()
				.filter((implementation) -> !keep.contains(implementation))
				.map(MetricsRun::disableExport).forEach(propertyValues::add);
		return propertyValues.toArray(new String[0]);
	}

	private static String disableExport(String implementation) {
		return "management.metrics.export." + implementation + ".enabled=false";
	}

}
