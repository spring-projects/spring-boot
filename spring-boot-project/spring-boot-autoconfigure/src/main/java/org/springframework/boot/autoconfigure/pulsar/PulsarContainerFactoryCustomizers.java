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

import org.springframework.boot.util.LambdaSafe;
import org.springframework.pulsar.config.PulsarContainerFactory;
import org.springframework.pulsar.core.PulsarConsumerFactory;

/**
 * Invokes the available {@link PulsarContainerFactoryCustomizer} instances in the context
 * for a given {@link PulsarConsumerFactory}.
 *
 * @author Chris Bono
 */
class PulsarContainerFactoryCustomizers {

	private final List<PulsarContainerFactoryCustomizer<?>> customizers;

	PulsarContainerFactoryCustomizers(List<? extends PulsarContainerFactoryCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList();
	}

	/**
	 * Customize the specified {@link PulsarContainerFactory}. Locates all
	 * {@link PulsarContainerFactoryCustomizer} beans able to handle the specified
	 * instance and invoke {@link PulsarContainerFactoryCustomizer#customize} on them.
	 * @param <T> the type of container factory
	 * @param containerFactory the container factory to customize
	 * @return the customized container factory
	 */
	@SuppressWarnings("unchecked")
	<T extends PulsarContainerFactory<?, ?>> T customize(T containerFactory) {
		LambdaSafe.callbacks(PulsarContainerFactoryCustomizer.class, this.customizers, containerFactory)
			.withLogger(PulsarContainerFactoryCustomizers.class)
			.invoke((customizer) -> customizer.customize(containerFactory));
		return containerFactory;
	}

}
