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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JVM metrics.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 2.1.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public class JvmMetricsAutoConfiguration {

	/**
	 * Creates a new instance of JvmGcMetrics if no other bean of the same type is
	 * present.
	 * @return the newly created JvmGcMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JvmGcMetrics jvmGcMetrics() {
		return new JvmGcMetrics();
	}

	/**
	 * Creates a new instance of JvmHeapPressureMetrics if no other bean of the same type
	 * is present.
	 * @return the newly created JvmHeapPressureMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JvmHeapPressureMetrics jvmHeapPressureMetrics() {
		return new JvmHeapPressureMetrics();
	}

	/**
	 * Creates a new instance of JvmMemoryMetrics if no other bean of the same type is
	 * present.
	 * @return the newly created JvmMemoryMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JvmMemoryMetrics jvmMemoryMetrics() {
		return new JvmMemoryMetrics();
	}

	/**
	 * Creates a new instance of JvmThreadMetrics if no other bean of the same type is
	 * present.
	 * @return the newly created JvmThreadMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JvmThreadMetrics jvmThreadMetrics() {
		return new JvmThreadMetrics();
	}

	/**
	 * Creates a new instance of ClassLoaderMetrics if no other bean of the same type is
	 * present in the application context. This bean is conditionally created only if
	 * there is no other bean of the same type already present.
	 * @return the newly created ClassLoaderMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public ClassLoaderMetrics classLoaderMetrics() {
		return new ClassLoaderMetrics();
	}

	/**
	 * Creates a new instance of JvmInfoMetrics if no other bean of the same type is
	 * present.
	 * @return the newly created JvmInfoMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JvmInfoMetrics jvmInfoMetrics() {
		return new JvmInfoMetrics();
	}

	/**
	 * Creates a new instance of JvmCompilationMetrics if no other bean of the same type
	 * is present.
	 * @return the newly created JvmCompilationMetrics instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public JvmCompilationMetrics jvmCompilationMetrics() {
		return new JvmCompilationMetrics();
	}

}
