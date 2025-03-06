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

import org.springframework.pulsar.config.PulsarContainerFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize a
 * {@link PulsarContainerFactory} before it is fully initialized, in particular to tune
 * its configuration.
 *
 * @param <T> the type of the {@link PulsarContainerFactory}
 * @author Chris Bono
 * @since 3.4.0
 */
@FunctionalInterface
public interface PulsarContainerFactoryCustomizer<T extends PulsarContainerFactory<?, ?>> {

	/**
	 * Customize the container factory.
	 * @param containerFactory the {@code PulsarContainerFactory} to customize
	 */
	void customize(T containerFactory);

}
