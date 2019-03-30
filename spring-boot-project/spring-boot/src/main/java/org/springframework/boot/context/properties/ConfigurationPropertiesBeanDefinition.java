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
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BeanDefinition} that is used for registering {@link ConfigurationProperties}
 * beans that are bound at creation time.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
final class ConfigurationPropertiesBeanDefinition extends GenericBeanDefinition {

	static ConfigurationPropertiesBeanDefinition from(
			ConfigurableListableBeanFactory beanFactory, String beanName, Class<?> type) {
		ConfigurationPropertiesBeanDefinition beanDefinition = new ConfigurationPropertiesBeanDefinition();
		beanDefinition.setBeanClass(type);
		beanDefinition.setInstanceSupplier(createBean(beanFactory, beanName, type));
		return beanDefinition;
	}

	private static <T> Supplier<T> createBean(ConfigurableListableBeanFactory beanFactory,
			String beanName, Class<T> type) {
		return () -> {
			ConfigurationProperties annotation = getAnnotation(type,
					ConfigurationProperties.class);
			Validated validated = getAnnotation(type, Validated.class);
			Annotation[] annotations = (validated != null)
					? new Annotation[] { annotation, validated }
					: new Annotation[] { annotation };
			Bindable<T> bindable = Bindable.of(type).withAnnotations(annotations);
			ConfigurationPropertiesBinder binder = beanFactory.getBean(
					ConfigurationPropertiesBinder.BEAN_NAME,
					ConfigurationPropertiesBinder.class);
			try {
				return binder.bind(bindable).orElseCreate(type);
			}
			catch (Exception ex) {
				throw new ConfigurationPropertiesBindException(beanName, type, annotation,
						ex);
			}
		};
	}

	private static <A extends Annotation> A getAnnotation(Class<?> type,
			Class<A> annotationType) {
		return AnnotationUtils.findAnnotation(type, annotationType);
	}

}
