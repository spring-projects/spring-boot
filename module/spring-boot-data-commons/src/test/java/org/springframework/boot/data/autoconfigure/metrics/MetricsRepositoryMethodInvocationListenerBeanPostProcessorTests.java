/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.autoconfigure.metrics;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.data.metrics.MetricsRepositoryMethodInvocationListener;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactoryCustomizer;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.function.SingletonSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsRepositoryMethodInvocationListenerBeanPostProcessor}.
 *
 * @author Phillip Webb
 */
class MetricsRepositoryMethodInvocationListenerBeanPostProcessorTests {

	private final MetricsRepositoryMethodInvocationListener listener = mock(
			MetricsRepositoryMethodInvocationListener.class);

	private final MetricsRepositoryMethodInvocationListenerBeanPostProcessor postProcessor = new MetricsRepositoryMethodInvocationListenerBeanPostProcessor(
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
