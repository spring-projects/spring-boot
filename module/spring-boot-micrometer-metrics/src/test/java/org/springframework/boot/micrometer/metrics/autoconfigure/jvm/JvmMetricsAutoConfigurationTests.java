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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
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
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmClassCountMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmMemoryUsedAfterLastGcMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmMemoryUsedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmThreadCountMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassCountMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryCommittedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryUsedAfterLastGcMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryUsedMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadCountMeterConvention;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link JvmMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class JvmMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
		.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresJvmMetrics() {
		this.contextRunner.run(assertMetricsBeans());
	}

	@Test
	void autoConfiguresJvmMetricsWithDefaultConventions() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.extracting("memoryUsedConvention")
				.isInstanceOf(MicrometerJvmMemoryUsedMeterConvention.class);
			assertThat(context.getBean(JvmMemoryMetrics.class)).extracting("memoryUsedAfterLastGcConvention")
				.isInstanceOf(MicrometerJvmMemoryUsedAfterLastGcMeterConvention.class);
			assertThat(context).hasSingleBean(JvmThreadMetrics.class)
				.getBean(JvmThreadMetrics.class)
				.extracting("threadCountConvention")
				.isInstanceOf(MicrometerJvmThreadCountMeterConvention.class);
			assertThat(context).hasSingleBean(ClassLoaderMetrics.class)
				.getBean(ClassLoaderMetrics.class)
				.extracting("classCountConvention")
				.isInstanceOf(MicrometerJvmClassCountMeterConvention.class);
		});
	}

	@Test
	void autoConfiguresJvmMetricsWithOpenTelemetryConventions() {
		this.contextRunner.withPropertyValues("management.observations.conventions=opentelemetry").run((context) -> {
			assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.extracting("memoryUsedConvention")
				.isInstanceOf(OpenTelemetryJvmMemoryUsedMeterConvention.class);
			assertThat(context.getBean(JvmMemoryMetrics.class)).extracting("memoryUsedAfterLastGcConvention")
				.isInstanceOf(OpenTelemetryJvmMemoryUsedAfterLastGcMeterConvention.class);
			assertThat(context).hasSingleBean(JvmThreadMetrics.class)
				.getBean(JvmThreadMetrics.class)
				.extracting("threadCountConvention")
				.isInstanceOf(OpenTelemetryJvmThreadCountMeterConvention.class);
			assertThat(context).hasSingleBean(ClassLoaderMetrics.class)
				.getBean(ClassLoaderMetrics.class)
				.extracting("classCountConvention")
				.isInstanceOf(OpenTelemetryJvmClassCountMeterConvention.class);
		});
	}

	@Test
	void allowsIndividualConventionToBeOverriddenWithOpenTelemetryConventions() {
		JvmMemoryUsedMeterConvention customUsed = mock(JvmMemoryUsedMeterConvention.class);
		this.contextRunner.withPropertyValues("management.observations.conventions=opentelemetry")
			.withBean(JvmMemoryUsedMeterConvention.class, () -> customUsed)
			.run((context) -> {
				assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
					.getBean(JvmMemoryMetrics.class)
					.hasFieldOrPropertyWithValue("memoryUsedConvention", customUsed);
				assertThat(context.getBean(JvmMemoryMetrics.class)).extracting("memoryCommittedConvention")
					.isInstanceOf(OpenTelemetryJvmMemoryCommittedMeterConvention.class);
				assertThat(context.getBean(JvmMemoryMetrics.class)).extracting("memoryUsedAfterLastGcConvention")
					.isInstanceOf(OpenTelemetryJvmMemoryUsedAfterLastGcMeterConvention.class);
			});
	}

	@Test
	void allowsCustomJvmGcMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmGcMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmGcMetrics")));
	}

	@Test
	void allowsCustomJvmHeapPressureMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmHeapPressureMetricsConfiguration.class)
			.run(assertMetricsBeans()
				.andThen((context) -> assertThat(context).hasBean("customJvmHeapPressureMetrics")));
	}

	@Test
	void allowsCustomJvmMemoryMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmMemoryMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmMemoryMetrics")));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void allowCustomJvmMemoryMeterConventionsToBeUsed() {
		JvmMemoryMeterConventions conventions = spy(new MicrometerJvmMemoryMeterConventions());
		this.contextRunner.withBean(JvmMemoryMeterConventions.class, () -> conventions).run((context) -> {
			assertThat(context).hasSingleBean(JvmMemoryMetrics.class);
			then(conventions).should().getMemoryUsedConvention();
			then(conventions).should().getMemoryCommittedConvention();
			then(conventions).should().getMemoryMaxConvention();
		});
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmMemoryMeterConventionsAndJvmMemoryUsedMeterConventionAreSet() {
		this.contextRunner.withBean(JvmMemoryMeterConventions.class, () -> mock(JvmMemoryMeterConventions.class))
			.withBean(JvmMemoryUsedMeterConvention.class, () -> mock(JvmMemoryUsedMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmMemoryMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmMemoryMeterConventionsAndJvmMemoryCommittedMeterConventionAreSet() {
		this.contextRunner.withBean(JvmMemoryMeterConventions.class, () -> mock(JvmMemoryMeterConventions.class))
			.withBean(JvmMemoryCommittedMeterConvention.class, () -> mock(JvmMemoryCommittedMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmMemoryMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmMemoryMeterConventionsAndJvmMemoryMaxMeterConventionAreSet() {
		this.contextRunner.withBean(JvmMemoryMeterConventions.class, () -> mock(JvmMemoryMeterConventions.class))
			.withBean(JvmMemoryMaxMeterConvention.class, () -> mock(JvmMemoryMaxMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmMemoryMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	void allowCustomJvmMemoryUsedMeterConventionToBeUsed() {
		JvmMemoryUsedMeterConvention memoryUsedConvention = mock(JvmMemoryUsedMeterConvention.class);
		this.contextRunner.withBean(JvmMemoryUsedMeterConvention.class, () -> memoryUsedConvention)
			.run((context) -> assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.hasFieldOrPropertyWithValue("memoryUsedConvention", memoryUsedConvention));
	}

	@Test
	void allowCustomJvmMemoryCommittedMeterConventionToBeUsed() {
		JvmMemoryCommittedMeterConvention memoryCommittedConvention = mock(JvmMemoryCommittedMeterConvention.class);
		this.contextRunner.withBean(JvmMemoryCommittedMeterConvention.class, () -> memoryCommittedConvention)
			.run((context) -> assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.hasFieldOrPropertyWithValue("memoryCommittedConvention", memoryCommittedConvention));
	}

	@Test
	void allowCustomJvmMemoryMaxMeterConventionToBeUsed() {
		JvmMemoryMaxMeterConvention memoryMaxConvention = mock(JvmMemoryMaxMeterConvention.class);
		this.contextRunner.withBean(JvmMemoryMaxMeterConvention.class, () -> memoryMaxConvention)
			.run((context) -> assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.hasFieldOrPropertyWithValue("memoryMaxConvention", memoryMaxConvention));
	}

	@Test
	void allowCustomJvmMemoryUsedAfterLastGcMeterConventionToBeUsed() {
		JvmMemoryUsedAfterLastGcMeterConvention memoryUsedAfterLastGcConvention = mock(
				JvmMemoryUsedAfterLastGcMeterConvention.class);
		this.contextRunner
			.withBean(JvmMemoryUsedAfterLastGcMeterConvention.class, () -> memoryUsedAfterLastGcConvention)
			.run((context) -> assertThat(context).hasSingleBean(JvmMemoryMetrics.class)
				.getBean(JvmMemoryMetrics.class)
				.hasFieldOrPropertyWithValue("memoryUsedAfterLastGcConvention", memoryUsedAfterLastGcConvention));
	}

	@Test
	void allowsCustomJvmThreadMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmThreadMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmThreadMetrics")));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void allowCustomJvmThreadMeterConventionsToBeUsed() {
		JvmThreadMeterConventions conventions = spy(new MicrometerJvmThreadMeterConventions(Tags.empty()));
		this.contextRunner.withBean(JvmThreadMeterConventions.class, () -> conventions).run((context) -> {
			assertThat(context).hasSingleBean(JvmThreadMetrics.class);
			then(conventions).should().threadCountConvention();
		});
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmThreadMeterConventionsAndJvmThreadCountMeterConventionAreSet() {
		this.contextRunner.withBean(JvmThreadMeterConventions.class, () -> mock(JvmThreadMeterConventions.class))
			.withBean(JvmThreadCountMeterConvention.class, () -> mock(JvmThreadCountMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmMemoryMeterConventions or JvmThreadCountMeterConvention should be set"));
	}

	@Test
	void allowCustomJJvmThreadCountMeterConventionToBeUsed() {
		JvmThreadCountMeterConvention threadCountConvention = mock(JvmThreadCountMeterConvention.class);
		this.contextRunner.withBean(JvmThreadCountMeterConvention.class, () -> threadCountConvention)
			.run((context) -> assertThat(context).hasSingleBean(JvmThreadMetrics.class)
				.getBean(JvmThreadMetrics.class)
				.hasFieldOrPropertyWithValue("threadCountConvention", threadCountConvention));
	}

	@Test
	void allowsCustomClassLoaderMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClassLoaderMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customClassLoaderMetrics")));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void allowCustomJvmClassLoadingMeterConventionsToBeUsed() {
		JvmClassLoadingMeterConventions conventions = spy(new MicrometerJvmClassLoadingMeterConventions());
		this.contextRunner.withBean(JvmClassLoadingMeterConventions.class, () -> conventions).run((context) -> {
			assertThat(context).hasSingleBean(ClassLoaderMetrics.class);
			then(conventions).should().currentClassCountConvention();
			then(conventions).should().loadedConvention();
			then(conventions).should().unloadedConvention();
		});
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmClassLoadingMeterConventionsAndJvmClassCountMeterConventionAreSet() {
		this.contextRunner
			.withBean(JvmClassLoadingMeterConventions.class, () -> mock(JvmClassLoadingMeterConventions.class))
			.withBean(JvmClassCountMeterConvention.class, () -> mock(JvmClassCountMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmClassLoadingMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmClassLoadingMeterConventionsAndJvmClassLoadedMeterConventionAreSet() {
		this.contextRunner
			.withBean(JvmClassLoadingMeterConventions.class, () -> mock(JvmClassLoadingMeterConventions.class))
			.withBean(JvmClassLoadedMeterConvention.class, () -> mock(JvmClassLoadedMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmClassLoadingMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	@Deprecated(since = "4.2.0", forRemoval = true)
	void shouldFailIfBothJvmClassLoadingMeterConventionsAndJvmClassUnloadedMeterConventionAreSet() {
		this.contextRunner
			.withBean(JvmClassLoadingMeterConventions.class, () -> mock(JvmClassLoadingMeterConventions.class))
			.withBean(JvmClassUnloadedMeterConvention.class, () -> mock(JvmClassUnloadedMeterConvention.class))
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining(
						"Either JvmClassLoadingMeterConventions or the interfaces that supersede it should be set"));
	}

	@Test
	void allowCustomJvmClassCountMeterConventionToBeUsed() {
		JvmClassCountMeterConvention classCountConvention = mock(JvmClassCountMeterConvention.class);
		this.contextRunner.withBean(JvmClassCountMeterConvention.class, () -> classCountConvention)
			.run((context) -> assertThat(context).hasSingleBean(ClassLoaderMetrics.class)
				.getBean(ClassLoaderMetrics.class)
				.hasFieldOrPropertyWithValue("classCountConvention", classCountConvention));
	}

	@Test
	void allowCustomJvmClassLoadedMeterConventionToBeUsed() {
		JvmClassLoadedMeterConvention classLoadedConvention = mock(JvmClassLoadedMeterConvention.class);
		this.contextRunner.withBean(JvmClassLoadedMeterConvention.class, () -> classLoadedConvention)
			.run((context) -> assertThat(context).hasSingleBean(ClassLoaderMetrics.class)
				.getBean(ClassLoaderMetrics.class)
				.hasFieldOrPropertyWithValue("classLoadedConvention", classLoadedConvention));
	}

	@Test
	void allowCustomJvmClassUnloadedMeterConventionToBeUsed() {
		JvmClassUnloadedMeterConvention classUnloadedConvention = mock(JvmClassUnloadedMeterConvention.class);
		this.contextRunner.withBean(JvmClassUnloadedMeterConvention.class, () -> classUnloadedConvention)
			.run((context) -> assertThat(context).hasSingleBean(ClassLoaderMetrics.class)
				.getBean(ClassLoaderMetrics.class)
				.hasFieldOrPropertyWithValue("classUnloadedConvention", classUnloadedConvention));
	}

	@Test
	void allowsCustomJvmInfoMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmInfoMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmInfoMetrics")));
	}

	@Test
	void allowsCustomJvmCompilationMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmCompilationMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmCompilationMetrics")));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void autoConfiguresJvmMetricsWithVirtualThreadsMetrics() {
		this.contextRunner.run(assertMetricsBeans()
			.andThen((context) -> assertThat(context).hasSingleBean(getVirtualThreadMetricsClass())));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void allowCustomVirtualThreadMetricsToBeUsed() {
		Class<MeterBinder> virtualThreadMetricsClass = getVirtualThreadMetricsClass();
		this.contextRunner
			.withBean("customVirtualThreadMetrics", virtualThreadMetricsClass,
					() -> BeanUtils.instantiateClass(virtualThreadMetricsClass))
			.run(assertMetricsBeans()
				.andThen((context) -> assertThat(context).hasSingleBean(getVirtualThreadMetricsClass())
					.hasBean("customVirtualThreadMetrics")));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldRegisterVirtualThreadMetricsRuntimeHints() {
		RuntimeHints hints = new RuntimeHints();
		new JvmMetricsAutoConfiguration.VirtualThreadMetricsRuntimeHintsRegistrar().registerHints(hints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference.of(getVirtualThreadMetricsClass()))
			.withMemberCategories(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)).accepts(hints);
	}

	private ContextConsumer<AssertableApplicationContext> assertMetricsBeans() {
		return (context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
			.hasSingleBean(JvmHeapPressureMetrics.class)
			.hasSingleBean(JvmMemoryMetrics.class)
			.hasSingleBean(JvmThreadMetrics.class)
			.hasSingleBean(ClassLoaderMetrics.class)
			.hasSingleBean(JvmInfoMetrics.class)
			.hasSingleBean(JvmCompilationMetrics.class);
	}

	@SuppressWarnings("unchecked")
	private static Class<MeterBinder> getVirtualThreadMetricsClass() {
		return (Class<MeterBinder>) ClassUtils
			.resolveClassName("io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics", null);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmGcMetricsConfiguration {

		@Bean
		JvmGcMetrics customJvmGcMetrics() {
			return new JvmGcMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmHeapPressureMetricsConfiguration {

		@Bean
		JvmHeapPressureMetrics customJvmHeapPressureMetrics() {
			return new JvmHeapPressureMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMetricsConfiguration {

		@Bean
		JvmMemoryMetrics customJvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmThreadMetricsConfiguration {

		@Bean
		JvmThreadMetrics customJvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClassLoaderMetricsConfiguration {

		@Bean
		ClassLoaderMetrics customClassLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmInfoMetricsConfiguration {

		@Bean
		JvmInfoMetrics customJvmInfoMetrics() {
			return new JvmInfoMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmCompilationMetricsConfiguration {

		@Bean
		JvmCompilationMetrics customJvmCompilationMetrics() {
			return new JvmCompilationMetrics();
		}

	}

}
