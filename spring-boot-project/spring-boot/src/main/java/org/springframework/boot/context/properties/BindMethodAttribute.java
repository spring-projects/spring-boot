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

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.AttributeAccessor;

/**
 * Allows a {@link BindMethod} value to be stored and retrieved from an
 * {@link AttributeAccessor}.
 *
 * @author Phillip Webb
 */
final class BindMethodAttribute {

	static final String NAME = BindMethod.class.getName();

	private BindMethodAttribute() {
	}

	static BindMethod get(ApplicationContext applicationContext, String beanName) {
		return (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext)
				? get(configurableApplicationContext.getBeanFactory(), beanName) : null;
	}

	static BindMethod get(ConfigurableListableBeanFactory beanFactory, String beanName) {
		return (!beanFactory.containsBeanDefinition(beanName)) ? null : get(beanFactory.getBeanDefinition(beanName));
	}

	static BindMethod get(BeanDefinitionRegistry beanDefinitionRegistry, String beanName) {
		return (!beanDefinitionRegistry.containsBeanDefinition(beanName)) ? null
				: get(beanDefinitionRegistry.getBeanDefinition(beanName));
	}

	static BindMethod get(AttributeAccessor attributes) {
		return (BindMethod) attributes.getAttribute(NAME);
	}

	static void set(AttributeAccessor attributes, BindMethod bindMethod) {
		attributes.setAttribute(NAME, bindMethod);
	}

}
