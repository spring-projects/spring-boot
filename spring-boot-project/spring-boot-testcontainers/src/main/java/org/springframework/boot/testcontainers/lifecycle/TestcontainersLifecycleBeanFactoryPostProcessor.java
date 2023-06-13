/*
 * Copyright 2012-2023 the original author or authors.
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
			}
		}
	}

}
