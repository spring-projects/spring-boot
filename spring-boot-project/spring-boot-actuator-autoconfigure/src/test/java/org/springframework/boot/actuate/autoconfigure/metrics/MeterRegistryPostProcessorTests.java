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

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MeterRegistry.Config;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MeterRegistryPostProcessor}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(MockitoExtension.class)
class MeterRegistryPostProcessorTests {

	private final MetricsProperties properties = new MetricsProperties();

	private final List<MeterRegistryCustomizer<?>> customizers = new ArrayList<>();

	private final List<MeterFilter> filters = new ArrayList<>();

	private final List<MeterBinder> binders = new ArrayList<>();

	@Mock
	private MeterRegistryCustomizer<MeterRegistry> mockCustomizer;

	@Mock
	private MeterFilter mockFilter;

	@Mock
	private MeterBinder mockBinder;

	@Mock
	private MeterRegistry mockRegistry;

	@Mock
	private Config mockConfig;

	MeterRegistryPostProcessorTests() {
		this.properties.setUseGlobalRegistry(false);
	}

	@Test
	void postProcessAndInitializeWhenCompositeAppliesCustomizer() {
		this.customizers.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(false,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		postProcessAndInitialize(processor, composite);
		then(this.mockCustomizer).should().customize(composite);
	}

	@Test
	void postProcessAndInitializeAppliesCustomizer() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.customizers.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		postProcessAndInitialize(processor, this.mockRegistry);
		then(this.mockCustomizer).should().customize(this.mockRegistry);
	}

	@Test
	void postProcessAndInitializeAppliesFilter() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.filters.add(this.mockFilter);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		postProcessAndInitialize(processor, this.mockRegistry);
		then(this.mockConfig).should().meterFilter(this.mockFilter);
	}

	@Test
	void postProcessAndInitializeBindsTo() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.binders.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		postProcessAndInitialize(processor, this.mockRegistry);
		then(this.mockBinder).should().bindTo(this.mockRegistry);
	}

	@Test
	void postProcessAndInitializeWhenCompositeBindsTo() {
		this.binders.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(false,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		postProcessAndInitialize(processor, composite);
		then(this.mockBinder).should().bindTo(composite);
	}

	@Test
	void postProcessAndInitializeWhenCompositeExistsDoesNotBindTo() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(false,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), null);
		postProcessAndInitialize(processor, this.mockRegistry);
		then(this.mockBinder).shouldHaveNoInteractions();
	}

	@Test
	void postProcessAndInitializeBeOrderedCustomizerThenFilterThenBindTo() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.customizers.add(this.mockCustomizer);
		this.filters.add(this.mockFilter);
		this.binders.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		postProcessAndInitialize(processor, this.mockRegistry);
		InOrder ordered = inOrder(this.mockBinder, this.mockConfig, this.mockCustomizer);
		then(this.mockCustomizer).should(ordered).customize(this.mockRegistry);
		then(this.mockConfig).should(ordered).meterFilter(this.mockFilter);
		then(this.mockBinder).should(ordered).bindTo(this.mockRegistry);
	}

	@Test
	void postProcessAndInitializeWhenUseGlobalRegistryTrueAddsToGlobalRegistry() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.properties.setUseGlobalRegistry(true);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		try {
			postProcessAndInitialize(processor, this.mockRegistry);
			assertThat(Metrics.globalRegistry.getRegistries()).contains(this.mockRegistry);
		}
		finally {
			Metrics.removeRegistry(this.mockRegistry);
		}
	}

	@Test
	void postProcessAndInitializeWhenUseGlobalRegistryFalseDoesNotAddToGlobalRegistry() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		postProcessAndInitialize(processor, this.mockRegistry);
		assertThat(Metrics.globalRegistry.getRegistries()).doesNotContain(this.mockRegistry);
	}

	@Test
	void postProcessDoesNotBindToUntilSingletonsInitialized() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.binders.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(true,
				createObjectProvider(this.properties), createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders));
		processor.postProcessAfterInitialization(this.mockRegistry, "meterRegistry");
		then(this.mockBinder).shouldHaveNoInteractions();
		processor.afterSingletonsInstantiated();
		then(this.mockBinder).should().bindTo(this.mockRegistry);
	}

	private void postProcessAndInitialize(MeterRegistryPostProcessor processor, MeterRegistry registry) {
		processor.postProcessAfterInitialization(registry, "meterRegistry");
		processor.afterSingletonsInstantiated();
	}

	@SuppressWarnings("unchecked")
	private <T> ObjectProvider<T> createObjectProvider(List<T> objects) {
		ObjectProvider<T> objectProvider = mock(ObjectProvider.class);
		given(objectProvider.orderedStream()).willReturn(objects.stream());
		return objectProvider;
	}

	@SuppressWarnings("unchecked")
	private <T> ObjectProvider<T> createObjectProvider(T object) {
		ObjectProvider<T> objectProvider = mock(ObjectProvider.class);
		given(objectProvider.getObject()).willReturn(object);
		return objectProvider;
	}

}
