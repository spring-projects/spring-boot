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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Utility class to memorize {@code @Bean} definition metadata during initialization of
 * the bean factory.
 *
 * @author Dave Syer
 * @since 1.1.0
 * @deprecated since 2.2.0 in favor of {@link ConfigurationPropertiesBean}
 */
@Deprecated
public class ConfigurationBeanFactoryMetadata implements ApplicationContextAware {

	/**
	 * The bean name that this class is registered with.
	 */
	public static final String BEAN_NAME = ConfigurationBeanFactoryMetadata.class.getName();

	private ConfigurableApplicationContext applicationContext;

	public <A extends Annotation> Map<String, Object> getBeansWithFactoryAnnotation(Class<A> type) {
		Map<String, Object> result = new HashMap<>();
		for (String name : this.applicationContext.getBeanFactory().getBeanDefinitionNames()) {
			if (findFactoryAnnotation(name, type) != null) {
				result.put(name, this.applicationContext.getBean(name));
			}
		}
		return result;
	}

	public <A extends Annotation> A findFactoryAnnotation(String beanName, Class<A> type) {
		Method method = findFactoryMethod(beanName);
		return (method != null) ? AnnotationUtils.findAnnotation(method, type) : null;
	}

	public Method findFactoryMethod(String beanName) {
		ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (beanDefinition instanceof RootBeanDefinition) {
				return ((RootBeanDefinition) beanDefinition).getResolvedFactoryMethod();
			}
		}
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	static void register(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			GenericBeanDefinition definition = new GenericBeanDefinition();
			definition.setBeanClass(ConfigurationBeanFactoryMetadata.class);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(ConfigurationBeanFactoryMetadata.BEAN_NAME, definition);
		}
	}

}
