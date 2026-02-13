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
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassUnloadedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryCommittedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMaxMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryUsedAfterLastGcMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryUsedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadCountMeterConvention;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
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
	@SuppressWarnings("deprecation")
	JvmMemoryMetrics jvmMemoryMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmMemoryMeterConventions> jvmMemoryMeterConventions,
			ObjectProvider<JvmMemoryUsedMeterConvention> jvmMemoryUsedMeterConvention,
			ObjectProvider<JvmMemoryCommittedMeterConvention> jvmMemoryCommittedMeterConvention,
			ObjectProvider<JvmMemoryMaxMeterConvention> jvmMemoryMaxMeterConvention,
			ObjectProvider<JvmMemoryUsedAfterLastGcMeterConvention> jvmMemoryUsedAfterLastGcMeterConvention) {
		JvmMemoryMetricsFactory factory = new JvmMemoryMetricsFactory(jvmMemoryUsedMeterConvention.getIfAvailable(),
				jvmMemoryCommittedMeterConvention.getIfAvailable(), jvmMemoryMaxMeterConvention.getIfAvailable(),
				jvmMemoryUsedAfterLastGcMeterConvention.getIfAvailable());
		JvmMemoryMeterConventions deprecatedConventions = jvmMemoryMeterConventions.getIfAvailable();
		if (deprecatedConventions != null && factory.hasConvention()) {
			throw new IllegalStateException("Either %s or the interfaces that supersede it should be set"
				.formatted(JvmMemoryMeterConventions.class.getSimpleName()));
		}
		return (deprecatedConventions != null) ? new JvmMemoryMetrics(Collections.emptyList(), deprecatedConventions)
				: factory.create(observationProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("deprecation")
	JvmThreadMetrics jvmThreadMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmThreadMeterConventions> jvmThreadMeterConventions,
			ObjectProvider<JvmThreadCountMeterConvention> jvmThreadCountMeterConvention) {
		JvmThreadMeterConventions deprecatedConventions = jvmThreadMeterConventions.getIfAvailable();
		JvmThreadCountMeterConvention convention = jvmThreadCountMeterConvention.getIfAvailable();
		if (deprecatedConventions != null && convention != null) {
			throw new IllegalStateException(
					"Either %s or %s should be set".formatted(JvmMemoryMeterConventions.class.getSimpleName(),
							JvmThreadCountMeterConvention.class.getSimpleName()));
		}
		if (deprecatedConventions != null) {
			return new JvmThreadMetrics(Collections.emptyList(), deprecatedConventions);
		}
		JvmThreadMetrics.Builder builder = JvmThreadMetrics.builder();
		if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
			builder.openTelemetryConventions();
		}
		PropertyMapper map = PropertyMapper.get();
		map.from(convention).to(builder::threadCountConvention);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("deprecation")
	ClassLoaderMetrics classLoaderMetrics(ObservationProperties observationProperties,
			ObjectProvider<JvmClassLoadingMeterConventions> jvmClassLoadingMeterConventions,
			ObjectProvider<JvmClassCountMeterConvention> jvmClassCountMeterConvention,
			ObjectProvider<JvmClassLoadedMeterConvention> jvmClassLoadedMeterConvention,
			ObjectProvider<JvmClassUnloadedMeterConvention> jvmClassUnloadedMeterConvention) {
		ClassLoaderMetricsFactory factory = new ClassLoaderMetricsFactory(jvmClassCountMeterConvention.getIfAvailable(),
				jvmClassLoadedMeterConvention.getIfAvailable(), jvmClassUnloadedMeterConvention.getIfAvailable());
		JvmClassLoadingMeterConventions deprecatedConventions = jvmClassLoadingMeterConventions.getIfAvailable();
		if (deprecatedConventions != null && factory.hasConvention()) {
			throw new IllegalStateException("Either %s or the interfaces that supersede it should be set"
				.formatted(JvmClassLoadingMeterConventions.class.getSimpleName()));
		}
		return (deprecatedConventions != null) ? new ClassLoaderMetrics(deprecatedConventions)
				: factory.create(observationProperties);
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

	private record JvmMemoryMetricsFactory(@Nullable JvmMemoryUsedMeterConvention memoryUsedConvention,
			@Nullable JvmMemoryCommittedMeterConvention memoryCommittedConvention,
			@Nullable JvmMemoryMaxMeterConvention memoryMaxConvention,
			@Nullable JvmMemoryUsedAfterLastGcMeterConvention memoryUsedAfterLastGcConvention) {

		boolean hasConvention() {
			return this.memoryUsedConvention != null || this.memoryCommittedConvention != null
					|| this.memoryMaxConvention != null || this.memoryUsedAfterLastGcConvention != null;
		}

		JvmMemoryMetrics create(ObservationProperties observationProperties) {
			JvmMemoryMetrics.Builder builder = JvmMemoryMetrics.builder();
			if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
				builder.openTelemetryConventions();
			}
			PropertyMapper map = PropertyMapper.get();
			map.from(this.memoryUsedConvention).to(builder::memoryUsedConvention);
			map.from(this.memoryCommittedConvention).to(builder::memoryCommittedConvention);
			map.from(this.memoryMaxConvention).to(builder::memoryMaxConvention);
			map.from(this.memoryUsedAfterLastGcConvention).to(builder::memoryUsedAfterLastGcConvention);
			return builder.build();
		}

	}

	private record ClassLoaderMetricsFactory(@Nullable JvmClassCountMeterConvention classCountConvention,
			@Nullable JvmClassLoadedMeterConvention classLoadedConvention,
			@Nullable JvmClassUnloadedMeterConvention classUnloadedConvention) {

		boolean hasConvention() {
			return this.classCountConvention != null || this.classLoadedConvention != null
					|| this.classUnloadedConvention != null;
		}

		ClassLoaderMetrics create(ObservationProperties observationProperties) {
			ClassLoaderMetrics.Builder builder = ClassLoaderMetrics.builder();
			if (observationProperties.getConventions() == ConventionsVariant.OPENTELEMETRY) {
				builder.openTelemetryConventions();
			}
			PropertyMapper map = PropertyMapper.get();
			map.from(this.classCountConvention).to(builder::classCountConvention);
			map.from(this.classLoadedConvention).to(builder::classLoadedConvention);
			map.from(this.classUnloadedConvention).to(builder::classUnloadedConvention);
			return builder.build();
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
