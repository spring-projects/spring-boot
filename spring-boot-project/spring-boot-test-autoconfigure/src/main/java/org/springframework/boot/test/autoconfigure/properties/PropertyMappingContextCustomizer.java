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

package org.springframework.boot.test.autoconfigure.properties;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
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

	/**
     * Constructs a new PropertyMappingContextCustomizer with the specified AnnotationsPropertySource.
     *
     * @param propertySource the AnnotationsPropertySource to be used for property mapping
     */
    PropertyMappingContextCustomizer(AnnotationsPropertySource propertySource) {
		this.propertySource = propertySource;
	}

	/**
     * Customize the application context by adding a property source and registering a bean post processor.
     * 
     * @param context the configurable application context
     * @param mergedContextConfiguration the merged context configuration
     */
    @Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		if (!this.propertySource.isEmpty()) {
			context.getEnvironment().getPropertySources().addFirst(this.propertySource);
		}
		context.getBeanFactory()
			.registerSingleton(PropertyMappingCheckBeanPostProcessor.class.getName(),
					new PropertyMappingCheckBeanPostProcessor());
	}

	/**
     * Compares this PropertyMappingContextCustomizer object to the specified object. 
     * The result is true if and only if the argument is not null and is of the same class as this object, 
     * and the propertySource of both objects are equal.
     * 
     * @param obj the object to compare this PropertyMappingContextCustomizer against
     * @return true if the given object represents a PropertyMappingContextCustomizer equivalent to this object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass())
				&& this.propertySource.equals(((PropertyMappingContextCustomizer) obj).propertySource);
	}

	/**
     * Returns the hash code value for this PropertyMappingContextCustomizer object.
     * The hash code is generated based on the hash code of the propertySource field.
     *
     * @return the hash code value for this PropertyMappingContextCustomizer object
     */
    @Override
	public int hashCode() {
		return this.propertySource.hashCode();
	}

	/**
	 * {@link BeanPostProcessor} to check that {@link PropertyMapping @PropertyMapping} is
	 * only used on test classes.
	 */
	static class PropertyMappingCheckBeanPostProcessor implements BeanPostProcessor {

		/**
         * This method is called before the initialization of a bean.
         * It checks if the bean has both @Component and @PropertyMapping annotations.
         * If both annotations are present, it throws an IllegalStateException.
         * 
         * @param bean The bean object being processed.
         * @param beanName The name of the bean.
         * @return The processed bean object.
         * @throws BeansException If an error occurs during bean processing.
         */
        @Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			Class<?> beanClass = bean.getClass();
			MergedAnnotations annotations = MergedAnnotations.from(beanClass, SearchStrategy.SUPERCLASS);
			Set<Class<?>> components = annotations.stream(Component.class)
				.map(this::getRoot)
				.collect(Collectors.toSet());
			Set<Class<?>> propertyMappings = annotations.stream(PropertyMapping.class)
				.map(this::getRoot)
				.collect(Collectors.toSet());
			if (!components.isEmpty() && !propertyMappings.isEmpty()) {
				throw new IllegalStateException("The @PropertyMapping " + getAnnotationsDescription(propertyMappings)
						+ " cannot be used in combination with the @Component "
						+ getAnnotationsDescription(components));
			}
			return bean;
		}

		/**
         * Returns the root class type of the given merged annotation.
         * 
         * @param annotation the merged annotation
         * @return the root class type of the merged annotation
         */
        private Class<?> getRoot(MergedAnnotation<?> annotation) {
			return annotation.getRoot().getType();
		}

		/**
         * Returns a description of the given set of annotations.
         *
         * @param annotations the set of annotations to describe
         * @return a string representation of the annotations
         */
        private String getAnnotationsDescription(Set<Class<?>> annotations) {
			StringBuilder result = new StringBuilder();
			for (Class<?> annotation : annotations) {
				if (!result.isEmpty()) {
					result.append(", ");
				}
				result.append('@').append(ClassUtils.getShortName(annotation));
			}
			result.insert(0, (annotations.size() != 1) ? "annotations " : "annotation ");
			return result.toString();
		}

		/**
         * This method is called after the initialization of a bean. It is a hook for post-processing the bean instance.
         * 
         * @param bean The bean instance that has been initialized.
         * @param beanName The name of the bean.
         * @return The processed bean instance.
         * @throws BeansException If any error occurs during the post-processing.
         */
        @Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

	}

}
