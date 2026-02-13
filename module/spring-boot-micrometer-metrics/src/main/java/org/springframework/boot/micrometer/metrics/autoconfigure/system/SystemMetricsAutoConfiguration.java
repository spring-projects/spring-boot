/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure.system;

import java.io.File;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuCountMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuLoadMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuTimeMeterConvention;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import org.springframework.boot.micrometer.metrics.system.DiskSpaceMetricsBinder;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties.ConventionsVariant;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for system metrics.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 4.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties({ MetricsProperties.class, ObservationProperties.class })
public final class SystemMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	UptimeMetrics uptimeMetrics() {
		return new UptimeMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	ProcessorMetrics processorMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmCpuCountMeterConvention> jvmCpuCountMeterConvention,
			ObjectProvider<JvmCpuLoadMeterConvention> jvmCpuLoadMeterConvention,
			ObjectProvider<JvmCpuTimeMeterConvention> jvmCpuTimeMeterConvention) {
		ProcessorMetrics.Builder builder = ProcessorMetrics.builder();
		if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
			builder.openTelemetryConventions();
		}
		jvmCpuCountMeterConvention.ifAvailable(builder::cpuCountConvention);
		jvmCpuLoadMeterConvention.ifAvailable(builder::cpuLoadConvention);
		jvmCpuTimeMeterConvention.ifAvailable(builder::cpuTimeConvention);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	FileDescriptorMetrics fileDescriptorMetrics() {
		return new FileDescriptorMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	DiskSpaceMetricsBinder diskSpaceMetrics(MetricsProperties properties) {
		List<File> paths = properties.getSystem().getDiskspace().getPaths();
		return new DiskSpaceMetricsBinder(paths, Tags.empty());
	}

}
