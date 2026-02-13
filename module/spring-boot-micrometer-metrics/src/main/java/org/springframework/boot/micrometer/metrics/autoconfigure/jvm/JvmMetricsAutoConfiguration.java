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
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassCountMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassUnloadedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryCommittedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMaxMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryUsedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadCountMeterConvention;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties.ConventionsVariant;
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
@EnableConfigurationProperties(ObservationProperties.class)
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
	JvmMemoryMetrics jvmMemoryMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmMemoryUsedMeterConvention> jvmMemoryUsedMeterConvention,
			ObjectProvider<JvmMemoryCommittedMeterConvention> jvmMemoryCommittedMeterConvention,
			ObjectProvider<JvmMemoryMaxMeterConvention> jvmMemoryMaxMeterConvention) {
		JvmMemoryMetrics.Builder builder = JvmMemoryMetrics.builder();
		if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
			builder.openTelemetryConventions();
		}
		jvmMemoryUsedMeterConvention.ifAvailable(builder::memoryUsedConvention);
		jvmMemoryCommittedMeterConvention.ifAvailable(builder::memoryCommittedConvention);
		jvmMemoryMaxMeterConvention.ifAvailable(builder::memoryMaxConvention);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	JvmThreadMetrics jvmThreadMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmThreadCountMeterConvention> jvmThreadCountMeterConvention) {
		JvmThreadMetrics.Builder builder = JvmThreadMetrics.builder();
		if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
			builder.openTelemetryConventions();
		}
		jvmThreadCountMeterConvention.ifAvailable(builder::threadCountConvention);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	ClassLoaderMetrics classLoaderMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmClassCountMeterConvention> jvmClassCountMeterConvention,
			ObjectProvider<JvmClassLoadedMeterConvention> jvmClassLoadedMeterConvention,
			ObjectProvider<JvmClassUnloadedMeterConvention> jvmClassUnloadedMeterConvention) {
		ClassLoaderMetrics.Builder builder = ClassLoaderMetrics.builder();
		if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
			builder.openTelemetryConventions();
		}
		jvmClassCountMeterConvention.ifAvailable(builder::classCountConvention);
		jvmClassLoadedMeterConvention.ifAvailable(builder::classLoadedConvention);
		jvmClassUnloadedMeterConvention.ifAvailable(builder::classUnloadedConvention);
		return builder.build();
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
