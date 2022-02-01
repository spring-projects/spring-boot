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

package org.springframework.boot.actuate.autoconfigure.metrics.data;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.metrics.data.MetricsRepositoryMethodInvocationListener;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactoryCustomizer;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.function.SingletonSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsRepositoryMethodInvocationListenerBeanPostProcessor} .
 *
 * @author Phillip Webb
 */
class MetricsRepositoryMethodInvocationListenerBeanPostProcessorTests {

	private MetricsRepositoryMethodInvocationListener listener = mock(MetricsRepositoryMethodInvocationListener.class);

	private MetricsRepositoryMethodInvocationListenerBeanPostProcessor postProcessor = new MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
			SingletonSupplier.of(this.listener));

	@Test
	@SuppressWarnings("rawtypes")
	void postProcessBeforeInitializationWhenRepositoryFactoryBeanSupportAddsListener() {
		RepositoryFactoryBeanSupport bean = mock(RepositoryFactoryBeanSupport.class);
		Object result = this.postProcessor.postProcessBeforeInitialization(bean, "name");
		assertThat(result).isSameAs(bean);
		ArgumentCaptor<RepositoryFactoryCustomizer> customizer = ArgumentCaptor
				.forClass(RepositoryFactoryCustomizer.class);
		then(bean).should().addRepositoryFactoryCustomizer(customizer.capture());
		RepositoryFactorySupport repositoryFactory = mock(RepositoryFactorySupport.class);
		customizer.getValue().customize(repositoryFactory);
		then(repositoryFactory).should().addInvocationListener(this.listener);
	}

	@Test
	void postProcessBeforeInitializationWhenOtherBeanDoesNothing() {
		Object bean = new Object();
		Object result = this.postProcessor.postProcessBeforeInitialization(bean, "name");
		assertThat(result).isSameAs(bean);
	}

}
