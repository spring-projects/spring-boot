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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

public final class MetricsContextBuilder {
	private MetricsContextBuilder() {
	}

	public static String[] onlyEnableImplementations(String... enabledImplementations) {
		Collection<String> allowed = Arrays.asList(enabledImplementations);

		return Stream.concat(
				Stream.of("management.metrics.use-global-registry=false"),
				Stream.of("atlas", "datadog", "ganglia", "graphite", "influx", "jmx", "prometheus",
						"statsd", "newrelic", "signalfx", "wavefront", "simple")
						.filter(impl -> !allowed.contains(impl))
						.map(impl -> "management.metrics.export." + impl + ".enabled=false"))
				.toArray(String[]::new);
	}

	public static ApplicationContextRunner contextRunner(String... enabledImplementations) {
		return new ApplicationContextRunner()
				.withPropertyValues(onlyEnableImplementations(enabledImplementations))
				.withUserConfiguration(DefaultRegistryConfiguration.class)
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class));
	}

	@Configuration
	static class DefaultRegistryConfiguration {
	}
}
