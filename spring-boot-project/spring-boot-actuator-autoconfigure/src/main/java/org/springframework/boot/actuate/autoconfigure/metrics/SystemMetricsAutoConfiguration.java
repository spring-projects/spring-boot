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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for system metrics.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public class SystemMetricsAutoConfiguration {

	@Bean
	@ConditionalOnProperty(value = "management.metrics.binders.uptime.enabled", matchIfMissing = true)
	@ConditionalOnMissingBean
	public UptimeMetrics uptimeMetrics() {
		return new UptimeMetrics();
	}

	@Bean
	@ConditionalOnProperty(value = "management.metrics.binders.processor.enabled", matchIfMissing = true)
	@ConditionalOnMissingBean
	public ProcessorMetrics processorMetrics() {
		return new ProcessorMetrics();
	}

	@Bean
	@ConditionalOnProperty(name = "management.metrics.binders.files.enabled", matchIfMissing = true)
	@ConditionalOnMissingBean
	public FileDescriptorMetrics fileDescriptorMetrics() {
		return new FileDescriptorMetrics();
	}

}
