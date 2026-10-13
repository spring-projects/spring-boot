/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.lifecycle;

import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link BeanFactoryPostProcessor} to prevent {@link AutoCloseable} destruction calls so
 * that {@link TestcontainersLifecycleBeanPostProcessor} can be smarter about which
 * containers to close.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see TestcontainersLifecycleApplicationContextInitializer
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class TestcontainersLifecycleBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		for (String beanName : beanFactory.getBeanNamesForType(Startable.class, false, false)) {
			try {
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
				String destroyMethodName = beanDefinition.getDestroyMethodName();
				if (destroyMethodName == null || AbstractBeanDefinition.INFER_METHOD.equals(destroyMethodName)) {
					beanDefinition.setDestroyMethodName("");
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}
	}

}
