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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Registers a bean definition for a type annotated with {@link ConfigurationProperties}
 * using the prefix of the annotation in the bean name.
 *
 * @author Madhura Bhave
 */
final class ConfigurationPropertiesBeanDefinitionRegistrar {

	private static final boolean KOTLIN_PRESENT = KotlinDetector.isKotlinPresent();

	private ConfigurationPropertiesBeanDefinitionRegistrar() {
	}

	public static void register(BeanDefinitionRegistry registry,
			ConfigurableListableBeanFactory beanFactory, Class<?> type) {
		String name = getName(type);
		if (!containsBeanDefinition(beanFactory, name)) {
			registerBeanDefinition(registry, beanFactory, name, type);
		}
	}

	private static String getName(Class<?> type) {
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(type,
				ConfigurationProperties.class);
		String prefix = (annotation != null) ? annotation.prefix() : "";
		return (StringUtils.hasText(prefix) ? prefix + "-" + type.getName()
				: type.getName());
	}

	private static boolean containsBeanDefinition(
			ConfigurableListableBeanFactory beanFactory, String name) {
		if (beanFactory.containsBeanDefinition(name)) {
			return true;
		}
		BeanFactory parent = beanFactory.getParentBeanFactory();
		if (parent instanceof ConfigurableListableBeanFactory) {
			return containsBeanDefinition((ConfigurableListableBeanFactory) parent, name);
		}
		return false;
	}

	private static void registerBeanDefinition(BeanDefinitionRegistry registry,
			ConfigurableListableBeanFactory beanFactory, String name, Class<?> type) {
		assertHasAnnotation(type);
		registry.registerBeanDefinition(name,
				createBeanDefinition(beanFactory, name, type));
	}

	private static void assertHasAnnotation(Class<?> type) {
		Assert.notNull(
				AnnotationUtils.findAnnotation(type, ConfigurationProperties.class),
				() -> "No " + ConfigurationProperties.class.getSimpleName()
						+ " annotation found on  '" + type.getName() + "'.");
	}

	private static BeanDefinition createBeanDefinition(
			ConfigurableListableBeanFactory beanFactory, String name, Class<?> type) {
		if (canBindAtCreationTime(type)) {
			return ConfigurationPropertiesBeanDefinition.from(beanFactory, name, type);
		}
		else {
			GenericBeanDefinition definition = new GenericBeanDefinition();
			definition.setBeanClass(type);
			return definition;
		}
	}

	private static boolean canBindAtCreationTime(Class<?> type) {
		List<Constructor<?>> constructors = determineConstructors(type);
		return (constructors.size() == 1 && constructors.get(0).getParameterCount() > 0);
	}

	private static List<Constructor<?>> determineConstructors(Class<?> type) {
		List<Constructor<?>> constructors = new ArrayList<>();
		if (KOTLIN_PRESENT && KotlinDetector.isKotlinType(type)) {
			Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
			if (primaryConstructor != null) {
				constructors.add(primaryConstructor);
			}
		}
		else {
			constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
		}
		return constructors;
	}

}
