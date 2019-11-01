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

package org.springframework.boot.context.properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * {@link BeanFactoryPostProcessor} to validate that regular bean definitions aren't
 * creating {@link ConstructorBinding} beans.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertiesBeanDefinitionValidator implements BeanFactoryPostProcessor, Ordered {

	private static final String BEAN_NAME = ConfigurationPropertiesBeanDefinitionValidator.class.getName();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			if (!(definition instanceof ConfigurationPropertiesValueObjectBeanDefinition)) {
				validate(beanFactory, beanName);
			}
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	private void validate(ConfigurableListableBeanFactory beanFactory, String beanName) {
		Class<?> beanClass = beanFactory.getType(beanName, false);
		if (beanClass != null && BindMethod.forType(beanClass) == BindMethod.VALUE_OBJECT) {
			throw new BeanCreationException(beanName,
					"@EnableConfigurationProperties or @ConfigurationPropertiesScan must be used to add "
							+ "@ConstructorBinding type " + beanClass.getName());
		}
	}

	/**
	 * Register a {@link ConfigurationPropertiesBeanDefinitionValidator} bean if one is
	 * not already registered.
	 * @param registry the bean definition registry
	 */
	static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			GenericBeanDefinition definition = new GenericBeanDefinition();
			definition.setBeanClass(ConfigurationPropertiesBeanDefinitionValidator.class);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		ConfigurationPropertiesBinder.register(registry);
	}

}
