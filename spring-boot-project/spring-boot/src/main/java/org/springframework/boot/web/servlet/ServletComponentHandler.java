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

package org.springframework.boot.web.servlet;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Abstract base class for handlers of Servlet components discovered through classpath
 * scanning.
 *
 * @author Andy Wilkinson
 */
abstract class ServletComponentHandler {

	private final Class<? extends Annotation> annotationType;

	private final TypeFilter typeFilter;

	/**
	 * Constructs a new ServletComponentHandler with the specified annotation type.
	 * @param annotationType the class object representing the annotation type
	 */
	protected ServletComponentHandler(Class<? extends Annotation> annotationType) {
		this.typeFilter = new AnnotationTypeFilter(annotationType);
		this.annotationType = annotationType;
	}

	/**
	 * Returns the type filter used by the ServletComponentHandler.
	 * @return the type filter used by the ServletComponentHandler
	 */
	TypeFilter getTypeFilter() {
		return this.typeFilter;
	}

	/**
	 * Extracts the URL patterns from the given attributes map.
	 * @param attributes the attributes map containing the URL patterns
	 * @return the extracted URL patterns
	 * @throws IllegalStateException if both the "urlPatterns" and "value" attributes are
	 * present
	 */
	protected String[] extractUrlPatterns(Map<String, Object> attributes) {
		String[] value = (String[]) attributes.get("value");
		String[] urlPatterns = (String[]) attributes.get("urlPatterns");
		if (urlPatterns.length > 0) {
			Assert.state(value.length == 0, "The urlPatterns and value attributes are mutually exclusive.");
			return urlPatterns;
		}
		return value;
	}

	/**
	 * Extracts the initialization parameters from the given attributes map.
	 * @param attributes the attributes map containing the initialization parameters
	 * @return a map of initialization parameters with their corresponding values
	 */
	protected final Map<String, String> extractInitParameters(Map<String, Object> attributes) {
		Map<String, String> initParameters = new HashMap<>();
		for (AnnotationAttributes initParam : (AnnotationAttributes[]) attributes.get("initParams")) {
			String name = (String) initParam.get("name");
			String value = (String) initParam.get("value");
			initParameters.put(name, value);
		}
		return initParameters;
	}

	/**
	 * Handles the given annotated bean definition by retrieving the annotation attributes
	 * and performing the necessary actions.
	 * @param beanDefinition the annotated bean definition to handle
	 * @param registry the bean definition registry
	 */
	void handle(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
		Map<String, Object> attributes = beanDefinition.getMetadata()
			.getAnnotationAttributes(this.annotationType.getName());
		if (attributes != null) {
			doHandle(attributes, beanDefinition, registry);
		}
	}

	/**
	 * Handles the processing of a servlet component.
	 *
	 * This method is responsible for handling the attributes, annotated bean definition,
	 * and bean definition registry associated with a servlet component. The
	 * implementation of this method should perform the necessary operations required for
	 * processing the servlet component.
	 * @param attributes a map containing the attributes associated with the servlet
	 * component
	 * @param beanDefinition the annotated bean definition representing the servlet
	 * component
	 * @param registry the bean definition registry used for registering the servlet
	 * component
	 */
	protected abstract void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry);

}
