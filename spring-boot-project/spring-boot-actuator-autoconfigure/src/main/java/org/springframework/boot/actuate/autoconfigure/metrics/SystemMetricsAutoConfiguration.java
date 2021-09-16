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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.io.File;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;

import org.springframework.boot.actuate.metrics.system.DiskSpaceMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for system metrics.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(MetricsProperties.class)
public class SystemMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public UptimeMetrics uptimeMetrics() {
		return new UptimeMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	public ProcessorMetrics processorMetrics() {
		return new ProcessorMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	public FileDescriptorMetrics fileDescriptorMetrics() {
		return new FileDescriptorMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	public DiskSpaceMetricsBinder diskSpaceMetrics(MetricsProperties properties) {
		List<File> paths = properties.getSystem().getDiskspace().getPaths();
		return new DiskSpaceMetricsBinder(paths, Tags.empty());
	}

}
