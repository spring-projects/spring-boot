/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactoryCustomizer;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link BeanPostProcessor} to apply a {@link MetricsRepositoryMethodInvocationListener}
 * to all {@link RepositoryFactorySupport repository factories}.
 *
 * @author Phillip Webb
 */
class MetricsRepositoryMethodInvocationListenerBeanPostProcessor implements BeanPostProcessor {

	private final RepositoryFactoryCustomizer customizer;

	MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
			SingletonSupplier<MetricsRepositoryMethodInvocationListener> listener) {
		this.customizer = new MetricsRepositoryFactoryCustomizer(listener);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RepositoryFactoryBeanSupport) {
			((RepositoryFactoryBeanSupport<?, ?, ?>) bean).addRepositoryFactoryCustomizer(this.customizer);
		}
		return bean;
	}

	private static final class MetricsRepositoryFactoryCustomizer implements RepositoryFactoryCustomizer {

		private final SingletonSupplier<MetricsRepositoryMethodInvocationListener> listenerSupplier;

		private MetricsRepositoryFactoryCustomizer(
				SingletonSupplier<MetricsRepositoryMethodInvocationListener> listenerSupplier) {
			this.listenerSupplier = listenerSupplier;
		}

		@Override
		public void customize(RepositoryFactorySupport repositoryFactory) {
			repositoryFactory.addInvocationListener(this.listenerSupplier.get());
		}

	}

}
