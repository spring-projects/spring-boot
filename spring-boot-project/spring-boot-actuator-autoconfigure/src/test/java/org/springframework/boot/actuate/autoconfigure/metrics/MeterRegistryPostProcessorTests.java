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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link MeterRegistryPostProcessor}.
 *
 * @author Phillip Webb
 */
public class MeterRegistryPostProcessorTests {

	private List<MeterBinder> binderBeans = new ArrayList<>();

	private List<MeterRegistryCustomizer<?>> customizerBeans = new ArrayList<>();

	private ObjectProvider<Collection<MeterBinder>> binders = TestObjectProvider
			.of(this.binderBeans);

	private ObjectProvider<Collection<MeterRegistryCustomizer<?>>> customizers = TestObjectProvider
			.of(this.customizerBeans);

	@Mock
	private MeterBinder mockBinder;

	@Mock
	private MeterRegistryCustomizer<MeterRegistry> mockCustomizer;

	@Mock
	private MeterRegistry mockRegistry;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void postProcessWhenNotRegistryShouldReturnBean() {
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, false);
		Object bean = new Object();
		String beanName = "name";
		assertThat(processor.postProcessBeforeInitialization(bean, beanName))
				.isEqualTo(bean);
		assertThat(processor.postProcessAfterInitialization(bean, beanName))
				.isEqualTo(bean);
	}

	@Test
	public void postProcessorWhenCompositeShouldSkip() {
		this.binderBeans.add(this.mockBinder);
		this.customizerBeans.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, false);
		assertThat(processor.postProcessAfterInitialization(new CompositeMeterRegistry(),
				"name"));
		verifyZeroInteractions(this.mockBinder, this.mockCustomizer);
	}

	@Test
	public void postProcessShouldApplyCustomizer() {
		this.customizerBeans.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, false);
		assertThat(processor.postProcessAfterInitialization(this.mockRegistry, "name"));
		verify(this.mockCustomizer).customize(this.mockRegistry);
	}

	@Test
	public void postProcessShouldApplyBinder() {
		this.binderBeans.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, false);
		assertThat(processor.postProcessAfterInitialization(this.mockRegistry, "name"));
		verify(this.mockBinder).bindTo(this.mockRegistry);
	}

	@Test
	public void postProcessShouldCustomizeBeforeApplyingBinder() {
		this.binderBeans.add(this.mockBinder);
		this.customizerBeans.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		InOrder ordered = inOrder(this.mockCustomizer, this.mockBinder);
		ordered.verify(this.mockCustomizer).customize(this.mockRegistry);
		ordered.verify(this.mockBinder).bindTo(this.mockRegistry);
	}

	@Test
	public void postProcessWhenAddToGlobalRegistryShouldAddToGlobalRegistry() {
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, true);
		try {
			processor.postProcessAfterInitialization(this.mockRegistry, "name");
			assertThat(Metrics.globalRegistry.getRegistries())
					.contains(this.mockRegistry);
		}
		finally {
			Metrics.removeRegistry(this.mockRegistry);
		}
	}

	@Test
	public void postProcessWhenNotAddToGlobalRegistryShouldAddToGlobalRegistry() {
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		assertThat(Metrics.globalRegistry.getRegistries())
				.doesNotContain(this.mockRegistry);
	}

	private static class TestObjectProvider<T> implements ObjectProvider<T> {

		private final T value;

		TestObjectProvider(T value) {
			this.value = value;
		}

		@Override
		public T getObject() throws BeansException {
			Assert.state(this.value != null, "No value");
			return this.value;
		}

		@Override
		public T getObject(Object... args) throws BeansException {
			return this.value;
		}

		@Override
		public T getIfAvailable() throws BeansException {
			return this.value;
		}

		@Override
		public T getIfUnique() throws BeansException {
			throw new UnsupportedOperationException();
		}

		public static <T> TestObjectProvider<T> of(T value) {
			return new TestObjectProvider<>(value);
		}

	}

}
