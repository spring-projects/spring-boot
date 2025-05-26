/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.DefaultPulsarReaderContainerFactory;
import org.springframework.pulsar.config.ListenerContainerFactory;
import org.springframework.pulsar.config.PulsarContainerFactory;
import org.springframework.pulsar.config.PulsarListenerContainerFactory;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarContainerFactoryCustomizers}.
 *
 * @author Chris Bono
 */
class PulsarContainerFactoryCustomizersTests {

	@Test
	void customizeWithNullCustomizersShouldDoNothing() {
		PulsarContainerFactory<?, ?> containerFactory = mock(PulsarContainerFactory.class);
		new PulsarContainerFactoryCustomizers(null).customize(containerFactory);
		then(containerFactory).shouldHaveNoInteractions();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void customizeSimplePulsarContainerFactory() {
		PulsarContainerFactoryCustomizers customizers = new PulsarContainerFactoryCustomizers(
				Collections.singletonList(new SimplePulsarContainerFactoryCustomizer()));
		PulsarContainerProperties containerProperties = new PulsarContainerProperties();
		ConcurrentPulsarListenerContainerFactory<String> pulsarContainerFactory = new ConcurrentPulsarListenerContainerFactory<>(
				mock(PulsarConsumerFactory.class), containerProperties);
		customizers.customize(pulsarContainerFactory);
		assertThat(pulsarContainerFactory.getContainerProperties().getSubscriptionName()).isEqualTo("my-subscription");
	}

	@Test
	void customizeShouldCheckGeneric() {
		List<TestCustomizer<?>> list = new ArrayList<>();
		list.add(new TestCustomizer<>());
		list.add(new TestPulsarListenersContainerFactoryCustomizer());
		list.add(new TestConcurrentPulsarListenerContainerFactoryCustomizer());
		PulsarContainerFactoryCustomizers customizers = new PulsarContainerFactoryCustomizers(list);

		customizers.customize(mock(PulsarContainerFactory.class));
		assertThat(list.get(0).getCount()).isOne();
		assertThat(list.get(1).getCount()).isZero();
		assertThat(list.get(2).getCount()).isZero();

		customizers.customize(mock(ConcurrentPulsarListenerContainerFactory.class));
		assertThat(list.get(0).getCount()).isEqualTo(2);
		assertThat(list.get(1).getCount()).isOne();
		assertThat(list.get(2).getCount()).isOne();

		customizers.customize(mock(DefaultReactivePulsarListenerContainerFactory.class));
		assertThat(list.get(0).getCount()).isEqualTo(3);
		assertThat(list.get(1).getCount()).isEqualTo(2);
		assertThat(list.get(2).getCount()).isOne();

		customizers.customize(mock(DefaultPulsarReaderContainerFactory.class));
		assertThat(list.get(0).getCount()).isEqualTo(4);
		assertThat(list.get(1).getCount()).isEqualTo(2);
		assertThat(list.get(2).getCount()).isOne();
	}

	static class SimplePulsarContainerFactoryCustomizer
			implements PulsarContainerFactoryCustomizer<ConcurrentPulsarListenerContainerFactory<?>> {

		@Override
		public void customize(ConcurrentPulsarListenerContainerFactory<?> containerFactory) {
			containerFactory.getContainerProperties().setSubscriptionName("my-subscription");
		}

	}

	/**
	 * Test customizer that will match all {@link PulsarListenerContainerFactory}.
	 *
	 * @param <T> the container factory type
	 */
	static class TestCustomizer<T extends PulsarContainerFactory<?, ?>> implements PulsarContainerFactoryCustomizer<T> {

		private int count;

		@Override
		public void customize(T pulsarContainerFactory) {
			this.count++;
		}

		int getCount() {
			return this.count;
		}

	}

	/**
	 * Test customizer that will match both
	 * {@link ConcurrentPulsarListenerContainerFactory} and
	 * {@link DefaultReactivePulsarListenerContainerFactory} as they both extend
	 * {@link ListenerContainerFactory}.
	 */
	static class TestPulsarListenersContainerFactoryCustomizer extends TestCustomizer<ListenerContainerFactory<?, ?>> {

	}

	/**
	 * Test customizer that will match only
	 * {@link ConcurrentPulsarListenerContainerFactory}.
	 */
	static class TestConcurrentPulsarListenerContainerFactoryCustomizer
			extends TestCustomizer<ConcurrentPulsarListenerContainerFactory<?>> {

	}

}
