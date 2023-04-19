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

	protected ServletComponentHandler(Class<? extends Annotation> annotationType) {
		this.typeFilter = new AnnotationTypeFilter(annotationType);
		this.annotationType = annotationType;
	}

	TypeFilter getTypeFilter() {
		return this.typeFilter;
	}

	protected String[] extractUrlPatterns(Map<String, Object> attributes) {
		String[] value = (String[]) attributes.get("value");
		String[] urlPatterns = (String[]) attributes.get("urlPatterns");
		if (urlPatterns.length > 0) {
			Assert.state(value.length == 0, "The urlPatterns and value attributes are mutually exclusive.");
			return urlPatterns;
		}
		return value;
	}

	protected final Map<String, String> extractInitParameters(Map<String, Object> attributes) {
		Map<String, String> initParameters = new HashMap<>();
		for (AnnotationAttributes initParam : (AnnotationAttributes[]) attributes.get("initParams")) {
			String name = (String) initParam.get("name");
			String value = (String) initParam.get("value");
			initParameters.put(name, value);
		}
		return initParameters;
	}

	void handle(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
		Map<String, Object> attributes = beanDefinition.getMetadata()
			.getAnnotationAttributes(this.annotationType.getName());
		if (attributes != null) {
			doHandle(attributes, beanDefinition, registry);
		}
	}

	protected abstract void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry);

}
