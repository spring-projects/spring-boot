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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.function.Supplier;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that exposes Git and build information as metrics.
 *
 * @author Matthias Friedrich
 * @since 3.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		ProjectInfoAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public class ProjectMetricsAutoConfiguration {

	@Bean
	@ConditionalOnBean(GitProperties.class)
	public MeterBinder gitInfoMetrics(GitProperties props) {
		return (registry) -> {
			Gauge.Builder<Supplier<Number>> builder = Gauge.builder("git.info", () -> 1L)
					.description("Project Git information").strongReference(true);

			addTag(builder, "branch", props.getBranch());
			addTag(builder, "id", props.getShortCommitId());
			addTag(builder, "time", props.getCommitTime());

			builder.register(registry);
		};
	}

	@Bean
	@ConditionalOnBean(BuildProperties.class)
	public MeterBinder buildInfoMetrics(BuildProperties props) {
		return (registry) -> {
			Gauge.Builder<Supplier<Number>> builder = Gauge.builder("build.info", () -> 1L)
					.description("Project build information").strongReference(true);

			addTag(builder, "name", props.getName());
			addTag(builder, "artifact", props.getArtifact());
			addTag(builder, "group", props.getGroup());
			addTag(builder, "version", props.getVersion());
			addTag(builder, "time", props.getTime());

			builder.register(registry);
		};
	}

	private static void addTag(Gauge.Builder<Supplier<Number>> builder, String name, Object value) {
		if (value != null) {
			builder.tag(name, String.valueOf(value));
		}
	}

}
