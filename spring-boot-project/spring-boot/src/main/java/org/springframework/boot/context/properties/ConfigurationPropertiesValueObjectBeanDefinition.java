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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BeanDefinition} that is used for registering
 * {@link ConfigurationProperties @ConfigurationProperties} value object beans that are
 * bound at creation time.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
final class ConfigurationPropertiesValueObjectBeanDefinition extends GenericBeanDefinition {

	private final BeanFactory beanFactory;

	private final String beanName;

	ConfigurationPropertiesValueObjectBeanDefinition(BeanFactory beanFactory, String beanName, Class<?> beanClass) {
		this.beanFactory = beanFactory;
		this.beanName = beanName;
		setBeanClass(beanClass);
		setInstanceSupplier(this::createBean);
	}

	private Object createBean() {
		ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(this.beanFactory);
		ResolvableType type = ResolvableType.forClass(getBeanClass());
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(getBeanClass(),
				ConfigurationProperties.class);
		Validated validated = AnnotationUtils.findAnnotation(getBeanClass(), Validated.class);
		Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
				: new Annotation[] { annotation };
		Bindable<Object> bindTarget = Bindable.of(type).withAnnotations(annotations);
		try {
			return binder.bindOrCreate(bindTarget);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(this.beanName, getBeanClass(), annotation, ex);
		}
	}

}
