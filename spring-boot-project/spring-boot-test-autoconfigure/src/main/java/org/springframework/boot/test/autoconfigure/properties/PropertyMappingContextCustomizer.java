/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.properties;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizer} to map annotation attributes to {@link Environment}
 * properties.
 *
 * @author Phillip Webb
 */
class PropertyMappingContextCustomizer implements ContextCustomizer {

	private final AnnotationsPropertySource propertySource;

	PropertyMappingContextCustomizer(AnnotationsPropertySource propertySource) {
		this.propertySource = propertySource;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		if (!this.propertySource.isEmpty()) {
			context.getEnvironment().getPropertySources().addFirst(this.propertySource);
		}
		context.getBeanFactory().registerSingleton(
				PropertyMappingCheckBeanPostProcessor.class.getName(),
				new PropertyMappingCheckBeanPostProcessor());
	}

	@Override
	public int hashCode() {
		return this.propertySource.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null && getClass() == obj.getClass() && this.propertySource
				.equals(((PropertyMappingContextCustomizer) obj).propertySource));
	}

	/**
	 * {@link BeanPostProcessor} to check that {@link PropertyMapping} is only used on
	 * test classes.
	 */
	static class PropertyMappingCheckBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			Class<?> beanClass = bean.getClass();
			Set<Class<?>> components = new LinkedHashSet<>();
			Set<Class<?>> propertyMappings = new LinkedHashSet<>();
			while (beanClass != null) {
				Annotation[] annotations = AnnotationUtils.getAnnotations(beanClass);
				if (annotations != null) {
					for (Annotation annotation : annotations) {
						if (isAnnotated(annotation, Component.class)) {
							components.add(annotation.annotationType());
						}
						if (isAnnotated(annotation, PropertyMapping.class)) {
							propertyMappings.add(annotation.annotationType());
						}
					}
				}
				beanClass = beanClass.getSuperclass();
			}
			if (!components.isEmpty() && !propertyMappings.isEmpty()) {
				throw new IllegalStateException("The @PropertyMapping "
						+ getAnnotationsDescription(propertyMappings)
						+ " cannot be used in combination with the @Component "
						+ getAnnotationsDescription(components));
			}
			return bean;
		}

		private boolean isAnnotated(Annotation element,
				Class<? extends Annotation> annotationType) {
			try {
				return element.annotationType().equals(annotationType) || AnnotationUtils
						.findAnnotation(element.annotationType(), annotationType) != null;
			}
			catch (Throwable ex) {
				return false;
			}
		}

		private String getAnnotationsDescription(Set<Class<?>> annotations) {
			StringBuilder result = new StringBuilder();
			for (Class<?> annotation : annotations) {
				result.append(result.length() == 0 ? "" : ", ");
				result.append("@" + ClassUtils.getShortName(annotation));
			}
			result.insert(0, annotations.size() == 1 ? "annotation " : "annotations ");
			return result.toString();
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

	}

}
