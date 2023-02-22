/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.data;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider;
import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener;
import org.springframework.boot.actuate.metrics.data.RepositoryTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult.State;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RepositoryMetricsAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class RepositoryMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(RepositoryMetricsAutoConfiguration.class));

	@Test
	void backsOffWhenMeterRegistryIsMissing() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RepositoryMetricsAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(RepositoryTagsProvider.class));
	}

	@Test
	void definesTagsProviderAndListenerWhenMeterRegistryIsPresent() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(DefaultRepositoryTagsProvider.class);
			assertThat(context).hasSingleBean(MetricsRepositoryMethodInvocationListener.class);
			assertThat(context).hasSingleBean(MetricsRepositoryMethodInvocationListenerBeanPostProcessor.class);
		});
	}

	@Test
	void tagsProviderBacksOff() {
		this.contextRunner.withUserConfiguration(TagsProviderConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DefaultRepositoryTagsProvider.class);
			assertThat(context).hasSingleBean(TestRepositoryTagsProvider.class);
		});
	}

	@Test
	void metricsRepositoryMethodInvocationListenerBacksOff() {
		this.contextRunner.withUserConfiguration(MetricsRepositoryMethodInvocationListenerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(MetricsRepositoryMethodInvocationListener.class);
				assertThat(context).hasSingleBean(TestMetricsRepositoryMethodInvocationListener.class);
			});
	}

	@Test
	void metricNameCanBeConfigured() {
		this.contextRunner.withPropertyValues("management.metrics.data.repository.metric-name=datarepo")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context, ExampleRepository.class);
				Timer timer = registry.get("datarepo").timer();
				assertThat(timer).isNotNull();
			});
	}

	@Test
	void autoTimeRequestsCanBeConfigured() {
		this.contextRunner
			.withPropertyValues("management.metrics.data.repository.autotime.enabled=true",
					"management.metrics.data.repository.autotime.percentiles=0.5,0.7")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context, ExampleRepository.class);
				Timer timer = registry.get("spring.data.repository.invocations").timer();
				HistogramSnapshot snapshot = timer.takeSnapshot();
				assertThat(snapshot.percentileValues()).hasSize(2);
				assertThat(snapshot.percentileValues()[0].percentile()).isEqualTo(0.5);
				assertThat(snapshot.percentileValues()[1].percentile()).isEqualTo(0.7);
			});
	}

	@Test
	void timerWorksWithTimedAnnotationsWhenAutoTimeRequestsIsFalse() {
		this.contextRunner.withPropertyValues("management.metrics.data.repository.autotime.enabled=false")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context, ExampleAnnotatedRepository.class);
				Collection<Meter> meters = registry.get("spring.data.repository.invocations").meters();
				assertThat(meters).hasSize(1);
				Meter meter = meters.iterator().next();
				assertThat(meter.getId().getTag("method")).isEqualTo("count");
			});
	}

	@Test
	void doesNotTriggerEarlyInitializationThatPreventsMeterBindersFromBindingMeters() {
		this.contextRunner.withUserConfiguration(MeterBinderConfiguration.class)
			.run((context) -> assertThat(context.getBean(MeterRegistry.class).find("binder.test").counter())
				.isNotNull());
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableApplicationContext context,
			Class<?> repositoryInterface) {
		MetricsRepositoryMethodInvocationListener listener = context
			.getBean(MetricsRepositoryMethodInvocationListener.class);
		ReflectionUtils.doWithLocalMethods(repositoryInterface, (method) -> {
			RepositoryMethodInvocationResult result = mock(RepositoryMethodInvocationResult.class);
			given(result.getState()).willReturn(State.SUCCESS);
			RepositoryMethodInvocation invocation = new RepositoryMethodInvocation(repositoryInterface, method, result,
					10);
			listener.afterInvocation(invocation);
		});
		return context.getBean(MeterRegistry.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TagsProviderConfiguration {

		@Bean
		TestRepositoryTagsProvider tagsProvider() {
			return new TestRepositoryTagsProvider();
		}

	}

	private static final class TestRepositoryTagsProvider implements RepositoryTagsProvider {

		@Override
		public Iterable<Tag> repositoryTags(RepositoryMethodInvocation invocation) {
			return Collections.emptyList();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MeterBinderConfiguration {

		@Bean
		MeterBinder meterBinder() {
			return (registry) -> registry.counter("binder.test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MetricsRepositoryMethodInvocationListenerConfiguration {

		@Bean
		MetricsRepositoryMethodInvocationListener metricsRepositoryMethodInvocationListener(
				ObjectFactory<MeterRegistry> registry, RepositoryTagsProvider tagsProvider) {
			return new TestMetricsRepositoryMethodInvocationListener(registry::getObject, tagsProvider);
		}

	}

	static class TestMetricsRepositoryMethodInvocationListener extends MetricsRepositoryMethodInvocationListener {

		TestMetricsRepositoryMethodInvocationListener(Supplier<MeterRegistry> registrySupplier,
				RepositoryTagsProvider tagsProvider) {
			super(registrySupplier, tagsProvider, "test", AutoTimer.DISABLED);
		}

	}

	interface ExampleRepository extends Repository<Example, Long> {

		long count();

	}

	interface ExampleAnnotatedRepository extends Repository<Example, Long> {

		@Timed
		long count();

		long delete();

	}

	static class Example {

	}

}
