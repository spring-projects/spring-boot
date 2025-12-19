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

package org.springframework.boot.micrometer.metrics.autoconfigure.jvm;

import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JVM metrics.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
public final class JvmMetricsAutoConfiguration {

	private static final String VIRTUAL_THREAD_METRICS_CLASS = "io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics";

	@Bean
	@ConditionalOnMissingBean
	JvmGcMetrics jvmGcMetrics() {
		return new JvmGcMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	JvmHeapPressureMetrics jvmHeapPressureMetrics() {
		return new JvmHeapPressureMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	JvmMemoryMetrics jvmMemoryMetrics(ObjectProvider<JvmMemoryMeterConventions> jvmMemoryMeterConventions) {
		JvmMemoryMeterConventions conventions = jvmMemoryMeterConventions.getIfAvailable();
		return (conventions != null) ? new JvmMemoryMetrics(Collections.emptyList(), conventions)
				: new JvmMemoryMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	JvmThreadMetrics jvmThreadMetrics(ObjectProvider<JvmThreadMeterConventions> jvmThreadMeterConventions) {
		JvmThreadMeterConventions conventions = jvmThreadMeterConventions.getIfAvailable();
		return (conventions != null) ? new JvmThreadMetrics(Collections.emptyList(), conventions)
				: new JvmThreadMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	ClassLoaderMetrics classLoaderMetrics(
			ObjectProvider<JvmClassLoadingMeterConventions> jvmClassLoadingMeterConventions) {
		JvmClassLoadingMeterConventions conventions = jvmClassLoadingMeterConventions.getIfAvailable();
		return (conventions != null) ? new ClassLoaderMetrics(conventions) : new ClassLoaderMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	JvmInfoMetrics jvmInfoMetrics() {
		return new JvmInfoMetrics();
	}

	@Bean
	@ConditionalOnMissingBean
	JvmCompilationMetrics jvmCompilationMetrics() {
		return new JvmCompilationMetrics();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = VIRTUAL_THREAD_METRICS_CLASS)
	static class VirtualThreadMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean(type = VIRTUAL_THREAD_METRICS_CLASS)
		@ImportRuntimeHints(VirtualThreadMetricsRuntimeHintsRegistrar.class)
		MeterBinder virtualThreadMetrics() throws ClassNotFoundException {
			Class<?> virtualThreadMetricsClass = ClassUtils.forName(VIRTUAL_THREAD_METRICS_CLASS,
					getClass().getClassLoader());
			return (MeterBinder) BeanUtils.instantiateClass(virtualThreadMetricsClass);
		}

	}

	static final class VirtualThreadMetricsRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerTypeIfPresent(classLoader, VIRTUAL_THREAD_METRICS_CLASS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		}

	}

}
