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

	/**
     * Constructs a new BindMethodAttribute.
     */
    private BindMethodAttribute() {
	}

	/**
     * Retrieves the {@link BindMethod} from the given {@link ApplicationContext} using the specified bean name.
     * 
     * @param applicationContext the {@link ApplicationContext} to retrieve the {@link BindMethod} from
     * @param beanName the name of the bean to retrieve
     * @return the {@link BindMethod} if found, or {@code null} if not found
     */
    static BindMethod get(ApplicationContext applicationContext, String beanName) {
		return (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext)
				? get(configurableApplicationContext.getBeanFactory(), beanName) : null;
	}

	/**
     * Retrieves the BindMethod for the specified bean name from the given ConfigurableListableBeanFactory.
     * 
     * @param beanFactory the ConfigurableListableBeanFactory to retrieve the BindMethod from
     * @param beanName the name of the bean to retrieve the BindMethod for
     * @return the BindMethod for the specified bean name, or null if the bean definition does not exist
     */
    static BindMethod get(ConfigurableListableBeanFactory beanFactory, String beanName) {
		return (!beanFactory.containsBeanDefinition(beanName)) ? null : get(beanFactory.getBeanDefinition(beanName));
	}

	/**
     * Retrieves the {@link BindMethod} associated with the specified bean name from the given {@link BeanDefinitionRegistry}.
     * 
     * @param beanDefinitionRegistry the registry containing the bean definitions
     * @param beanName the name of the bean to retrieve the bind method for
     * @return the bind method associated with the specified bean name, or {@code null} if the bean definition does not exist
     */
    static BindMethod get(BeanDefinitionRegistry beanDefinitionRegistry, String beanName) {
		return (!beanDefinitionRegistry.containsBeanDefinition(beanName)) ? null
				: get(beanDefinitionRegistry.getBeanDefinition(beanName));
	}

	/**
     * Retrieves the BindMethod object associated with the given AttributeAccessor.
     * 
     * @param attributes the AttributeAccessor containing the BindMethod object
     * @return the BindMethod object associated with the given AttributeAccessor
     */
    static BindMethod get(AttributeAccessor attributes) {
		return (BindMethod) attributes.getAttribute(NAME);
	}

	/**
     * Sets the attribute with the given name and bind method in the provided attribute accessor.
     *
     * @param attributes the attribute accessor to set the attribute in
     * @param bindMethod the bind method to set as the attribute value
     */
    static void set(AttributeAccessor attributes, BindMethod bindMethod) {
		attributes.setAttribute(NAME, bindMethod);
	}

}
