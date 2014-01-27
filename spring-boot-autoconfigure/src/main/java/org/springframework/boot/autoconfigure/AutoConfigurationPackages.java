/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * Class for storing auto-configuration packages for reference later (e.g. by JPA entity
 * scanner).
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AutoConfigurationPackages {

	private static final String BEAN = AutoConfigurationPackages.class.getName();

	/**
	 * Return the auto-configuration base packages for the given bean factory
	 * @param beanFactory the source bean factory
	 * @return a list of auto-configuration packages
	 * @throws IllegalStateException if auto-configuration is not enabled
	 */
	public static List<String> get(BeanFactory beanFactory) {
		// Currently we only store a single base package, but we return a list to
		// allow this to change in the future if needed
		try {
			return Collections.singletonList(beanFactory.getBean(BEAN, BasePackage.class)
					.toString());
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new IllegalStateException(
					"Unable to retrieve @EnableAutoConfiguration base packages");
		}
	}

	static void set(BeanDefinitionRegistry registry, String packageName) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(BasePackage.class);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				packageName);
		beanDefinition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(BEAN, beanDefinition);
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
	 * configuration.
	 */
	@Order(Ordered.HIGHEST_PRECEDENCE)
	static class Registrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			set(registry,
					ClassUtils.getPackageName(importingClassMetadata.getClassName()));
		}

	}

	/**
	 * Holder for the base package.
	 */
	final static class BasePackage {

		private final String name;

		public BasePackage(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

}
