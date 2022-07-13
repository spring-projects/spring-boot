/*
 * Copyright 2019-2021 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;

/**
 * Helper class to programmatically bind configuration properties that use constructor
 * injection.
 *
 * @author Stephane Nicoll
 * @since 3.0.0
 * @see ConstructorBinding
 */
public abstract class ConstructorBound {

	/**
	 * Create an immutable {@link ConfigurationProperties} instance for the specified
	 * {@code beanName} and {@code beanType} using the specified {@link BeanFactory}.
	 * @param beanFactory the bean factory to use
	 * @param beanName the name of the bean
	 * @param beanType the type of the bean
	 * @return an instance from the specified bean
	 */
	public static Object from(BeanFactory beanFactory, String beanName, Class<?> beanType) {
		ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.forValueObject(beanType, beanName);
		ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(beanFactory);
		try {
			return binder.bindOrCreate(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

}
