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

package org.springframework.boot.metrics.autoconfigure.jvm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
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

/**
 * Tests for {@link JvmMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class JvmMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(MeterRegistry.class, () -> new SimpleMeterRegistry())
		.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresJvmMetrics() {
		this.contextRunner.run(assertMetricsBeans());
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
	void allowsCustomJvmThreadMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmThreadMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmThreadMetrics")));
	}

	@Test
	void allowsCustomClassLoaderMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClassLoaderMetricsConfiguration.class)
			.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customClassLoaderMetrics")));
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
	void doesntAutoConfigureJvmMetricsWithVirtualThreadsMetricsIfVirtualThreadsAreDisabled() {
		this.contextRunner.run(assertMetricsBeans()
			.andThen((context) -> assertThat(context).doesNotHaveBean(getVirtualThreadMetricsClass())));
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void autoConfiguresJvmMetricsWithVirtualThreadsMetricsIfVirtualThreadsAreEnabled() {
		this.contextRunner.withPropertyValues("spring.threads.virtual.enabled=true")
			.run(assertMetricsBeans()
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
