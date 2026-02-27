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

package org.springframework.boot.data.autoconfigure.metrics;

import java.util.function.Supplier;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactoryCustomizer;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link BeanPostProcessor} to apply an {@link ObservedRepositoryMethodInterceptor} to
 * all {@link RepositoryFactorySupport repository factories}.
 *
 * @author Kwonneung Lee
 */
class ObservedRepositoryMethodInterceptorBeanPostProcessor implements BeanPostProcessor {

	private final RepositoryFactoryCustomizer customizer;

	ObservedRepositoryMethodInterceptorBeanPostProcessor(
			SingletonSupplier<ObservationRegistry> observationRegistry) {
		this.customizer = new ObservedRepositoryFactoryCustomizer(observationRegistry);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RepositoryFactoryBeanSupport) {
			((RepositoryFactoryBeanSupport<?, ?, ?>) bean).addRepositoryFactoryCustomizer(this.customizer);
		}
		return bean;
	}

	private record ObservedRepositoryFactoryCustomizer(
			Supplier<ObservationRegistry> registrySupplier) implements RepositoryFactoryCustomizer {

		@Override
			public void customize(RepositoryFactorySupport repositoryFactory) {
				repositoryFactory.addRepositoryProxyPostProcessor(
						(factory, repositoryInformation) -> factory.addAdvice(
								new ObservedRepositoryMethodInterceptor(this.registrySupplier,
										repositoryInformation.getRepositoryInterface())));
			}

		}

}
