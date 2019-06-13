/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import java.lang.reflect.Modifier;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextFactory;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;

/**
 * A {@link ManagementContextFactory} for reactive web applications.
 *
 * @author Andy Wilkinson
 */
class ReactiveManagementContextFactory implements ManagementContextFactory {

	@Override
	public ConfigurableWebServerApplicationContext createManagementContext(ApplicationContext parent,
			Class<?>... configClasses) {
		AnnotationConfigReactiveWebServerApplicationContext child = new AnnotationConfigReactiveWebServerApplicationContext();
		child.setParent(parent);
		Class<?>[] combinedClasses = ObjectUtils.addObjectToArray(configClasses,
				ReactiveWebServerFactoryAutoConfiguration.class);
		child.register(combinedClasses);
		registerReactiveWebServerFactory(parent, child);
		return child;
	}

	private void registerReactiveWebServerFactory(ApplicationContext parent,
			AnnotationConfigReactiveWebServerApplicationContext childContext) {
		try {
			ConfigurableListableBeanFactory beanFactory = childContext.getBeanFactory();
			if (beanFactory instanceof BeanDefinitionRegistry) {
				BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
				registry.registerBeanDefinition("ReactiveWebServerFactory",
						new RootBeanDefinition(determineReactiveWebServerFactoryClass(parent)));
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ignore and assume auto-configuration
		}
	}

	private Class<?> determineReactiveWebServerFactoryClass(ApplicationContext parent)
			throws NoSuchBeanDefinitionException {
		Class<?> factoryClass = parent.getBean(ReactiveWebServerFactory.class).getClass();
		if (cannotBeInstantiated(factoryClass)) {
			throw new FatalBeanException("ReactiveWebServerFactory implementation " + factoryClass.getName()
					+ " cannot be instantiated. " + "To allow a separate management port to be used, a top-level class "
					+ "or static inner class should be used instead");
		}
		return factoryClass;
	}

	private boolean cannotBeInstantiated(Class<?> factoryClass) {
		return factoryClass.isLocalClass()
				|| (factoryClass.isMemberClass() && !Modifier.isStatic(factoryClass.getModifiers()))
				|| factoryClass.isAnonymousClass();
	}

}
