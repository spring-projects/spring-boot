/*
 * Copyright 2012-2020 the original author or authors.
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * {@link BeanDefinition} that is used for registering
 * {@link ConfigurationProperties @ConfigurationProperties} value object beans that are
 * bound at creation time.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Phillip Webb
 */
final class ConfigurationPropertiesValueObjectBeanDefinition extends ConfigurationPropertiesBeanDefinition {

	private final BeanFactory beanFactory;

	private final String beanName;

	private final boolean deduceBindConstructor;

	ConfigurationPropertiesValueObjectBeanDefinition(BeanFactory beanFactory, String beanName, Class<?> beanClass,
			MergedAnnotation<ConfigurationProperties> annotation, boolean deduceBindConstructor) {
		super(beanClass, annotation);
		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.deduceBindConstructor = deduceBindConstructor;
		setInstanceSupplier(this::createBean);
	}

	boolean isDeduceBindConstructor() {
		return this.deduceBindConstructor;
	}

	private Object createBean() {
		ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.forValueObject(getBeanClass(), this.beanName,
				getAnnotation(this), this.deduceBindConstructor);
		ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(this.beanFactory);
		try {
			return binder.bindOrCreate(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

}
