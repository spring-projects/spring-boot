/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * Utility class to memoize <code>@Bean</code> definition meta data during initialization
 * of the bean factory.
 * 
 * @author Dave Syer
 */
public class ConfigurationBeanFactoryMetaData implements BeanFactoryPostProcessor {

	private ConfigurableListableBeanFactory beanFactory;

	private Map<String, MetaData> beans = new HashMap<String, MetaData>();

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		this.beanFactory = beanFactory;
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			String method = definition.getFactoryMethodName();
			String bean = definition.getFactoryBeanName();
			if (method != null && bean != null) {
				this.beans.put(name, new MetaData(bean, method));
			}
		}
	}

	public <A extends Annotation> Map<String, Object> getBeansWithFactoryAnnotation(
			Class<A> type) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String name : this.beans.keySet()) {
			if (findFactoryAnnotation(name, type) != null) {
				result.put(name, this.beanFactory.getBean(name));
			}
		}
		return result;
	}

	public <A extends Annotation> A findFactoryAnnotation(String beanName, Class<A> type) {
		Method method = findFactoryMethod(beanName);
		if (method != null) {
			return AnnotationUtils.findAnnotation(method, type);
		}
		return null;
	}

	private Method findFactoryMethod(String beanName) {
		if (!this.beans.containsKey(beanName)) {
			return null;
		}
		final AtomicReference<Method> found = new AtomicReference<Method>();
		MetaData meta = this.beans.get(beanName);
		final String factory = meta.getMethod();
		Class<?> type = this.beanFactory.getType(meta.getBean());
		ReflectionUtils.doWithMethods(type, new MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException,
					IllegalAccessException {
				if (method.getName().equals(factory)) {
					found.set(method);
				}
			}
		});
		return found.get();
	}

	private static class MetaData {

		private String bean;

		private String method;

		public MetaData(String bean, String method) {
			this.bean = bean;
			this.method = method;
		}

		public String getBean() {
			return this.bean;
		}

		public String getMethod() {
			return this.method;
		}

	}

}
