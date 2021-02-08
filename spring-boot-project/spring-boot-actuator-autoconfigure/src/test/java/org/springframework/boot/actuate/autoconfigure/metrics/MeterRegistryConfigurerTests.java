/*
 * Copyright 2012-2020 the original author or authors.
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link MeterRegistryConfigurer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(MockitoExtension.class)
class MeterRegistryConfigurerTests {

	private List<MeterBinder> binders = new ArrayList<>();

	private List<MeterFilter> filters = new ArrayList<>();

	private List<MeterRegistryCustomizer<?>> customizers = new ArrayList<>();

	@Mock
	private MeterBinder mockBinder;

	@Mock
	private MeterFilter mockFilter;

	@Mock
	private MeterRegistryCustomizer<MeterRegistry> mockCustomizer;

	@Mock
	private MeterRegistry mockRegistry;

	@Mock
	private Config mockConfig;

	@Test
	void configureWhenCompositeShouldApplyCustomizer() {
		this.customizers.add(this.mockCustomizer);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, false);
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		configurer.configure(composite);
		verify(this.mockCustomizer).customize(composite);
	}

	@Test
	void configureShouldApplyCustomizer() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.customizers.add(this.mockCustomizer);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, false);
		configurer.configure(this.mockRegistry);
		verify(this.mockCustomizer).customize(this.mockRegistry);
	}

	@Test
	void configureShouldApplyFilter() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.filters.add(this.mockFilter);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, false);
		configurer.configure(this.mockRegistry);
		verify(this.mockConfig).meterFilter(this.mockFilter);
	}

	@Test
	void configureShouldApplyBinder() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.binders.add(this.mockBinder);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, false);
		configurer.configure(this.mockRegistry);
		verify(this.mockBinder).bindTo(this.mockRegistry);
	}

	@Test
	void configureShouldApplyBinderToComposite() {
		this.binders.add(this.mockBinder);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, true);
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		configurer.configure(composite);
		verify(this.mockBinder).bindTo(composite);
	}

	@Test
	void configureShouldNotApplyBinderWhenCompositeExists() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), null, false, true);
		configurer.configure(this.mockRegistry);
		verifyNoInteractions(this.mockBinder);
	}

	@Test
	void configureShouldBeCalledInOrderCustomizerFilterBinder() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		this.customizers.add(this.mockCustomizer);
		this.filters.add(this.mockFilter);
		this.binders.add(this.mockBinder);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, false);
		configurer.configure(this.mockRegistry);
		InOrder ordered = inOrder(this.mockBinder, this.mockConfig, this.mockCustomizer);
		ordered.verify(this.mockCustomizer).customize(this.mockRegistry);
		ordered.verify(this.mockConfig).meterFilter(this.mockFilter);
		ordered.verify(this.mockBinder).bindTo(this.mockRegistry);
	}

	@Test
	void configureWhenAddToGlobalRegistryShouldAddToGlobalRegistry() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), true, false);
		try {
			configurer.configure(this.mockRegistry);
			assertThat(Metrics.globalRegistry.getRegistries()).contains(this.mockRegistry);
		}
		finally {
			Metrics.removeRegistry(this.mockRegistry);
		}
	}

	@Test
	void configureWhenNotAddToGlobalRegistryShouldAddToGlobalRegistry() {
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
		MeterRegistryConfigurer configurer = new MeterRegistryConfigurer(createObjectProvider(this.customizers),
				createObjectProvider(this.filters), createObjectProvider(this.binders), false, false);
		configurer.configure(this.mockRegistry);
		assertThat(Metrics.globalRegistry.getRegistries()).doesNotContain(this.mockRegistry);
	}

	@SuppressWarnings("unchecked")
	private <T> ObjectProvider<T> createObjectProvider(List<T> objects) {
		ObjectProvider<T> objectProvider = mock(ObjectProvider.class);
		given(objectProvider.orderedStream()).willReturn(objects.stream());
		return objectProvider;
	}

}
