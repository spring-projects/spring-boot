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

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Delegate used by {@link EnableConfigurationPropertiesRegistrar} and
 * {@link ConfigurationPropertiesScanRegistrar} to register a bean definition for a
 * {@link ConfigurationProperties @ConfigurationProperties} class.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
final class ConfigurationPropertiesBeanRegistrar {

	private final BeanDefinitionRegistry registry;

	private final BeanFactory beanFactory;

	/**
	 * Constructs a new ConfigurationPropertiesBeanRegistrar with the specified
	 * BeanDefinitionRegistry.
	 * @param registry the BeanDefinitionRegistry to use for registering bean definitions
	 */
	ConfigurationPropertiesBeanRegistrar(BeanDefinitionRegistry registry) {
		this.registry = registry;
		this.beanFactory = (BeanFactory) this.registry;
	}

	/**
	 * Registers a class as a configuration properties bean.
	 * @param type the class to be registered
	 */
	void register(Class<?> type) {
		MergedAnnotation<ConfigurationProperties> annotation = MergedAnnotations
			.from(type, SearchStrategy.TYPE_HIERARCHY)
			.get(ConfigurationProperties.class);
		register(type, annotation);
	}

	/**
	 * Registers a bean definition for the given type with the specified configuration
	 * properties annotation.
	 * @param type the class type of the bean
	 * @param annotation the merged annotation containing the configuration properties
	 * information
	 */
	void register(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
		String name = getName(type, annotation);
		if (!containsBeanDefinition(name)) {
			registerBeanDefinition(name, type, annotation);
		}
	}

	/**
	 * Returns the name of the configuration properties bean based on the provided type
	 * and annotation. If the annotation is present, it retrieves the prefix value from
	 * the annotation and appends it to the type name. If the prefix is not empty, it
	 * concatenates the prefix and type name with a hyphen. If the prefix is empty, it
	 * returns just the type name.
	 * @param type the type of the configuration properties bean
	 * @param annotation the merged annotation containing the prefix value
	 * @return the name of the configuration properties bean
	 */
	private String getName(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
		String prefix = annotation.isPresent() ? annotation.getString("prefix") : "";
		return (StringUtils.hasText(prefix) ? prefix + "-" + type.getName() : type.getName());
	}

	/**
	 * Check if the specified bean definition is contained in the bean factory.
	 * @param name the name of the bean definition to check
	 * @return {@code true} if the bean definition is contained, {@code false} otherwise
	 */
	private boolean containsBeanDefinition(String name) {
		return containsBeanDefinition(this.beanFactory, name);
	}

	/**
	 * Checks if the given bean factory contains a bean definition with the specified
	 * name.
	 * @param beanFactory the bean factory to check
	 * @param name the name of the bean definition to check for
	 * @return true if the bean factory contains the bean definition, false otherwise
	 */
	private boolean containsBeanDefinition(BeanFactory beanFactory, String name) {
		if (beanFactory instanceof ListableBeanFactory listableBeanFactory
				&& listableBeanFactory.containsBeanDefinition(name)) {
			return true;
		}
		if (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
			return containsBeanDefinition(hierarchicalBeanFactory.getParentBeanFactory(), name);
		}
		return false;
	}

	/**
	 * Registers a bean definition for a configuration properties bean.
	 * @param beanName the name of the bean
	 * @param type the class of the bean
	 * @param annotation the merged annotation containing the
	 * {@link ConfigurationProperties} annotation
	 * @throws IllegalStateException if the {@link ConfigurationProperties} annotation is
	 * not present on the bean class
	 */
	private void registerBeanDefinition(String beanName, Class<?> type,
			MergedAnnotation<ConfigurationProperties> annotation) {
		Assert.state(annotation.isPresent(), () -> "No " + ConfigurationProperties.class.getSimpleName()
				+ " annotation found on  '" + type.getName() + "'.");
		this.registry.registerBeanDefinition(beanName, createBeanDefinition(beanName, type));
	}

	/**
	 * Creates a bean definition for the given bean name and type.
	 * @param beanName the name of the bean
	 * @param type the class type of the bean
	 * @return the created bean definition
	 */
	private BeanDefinition createBeanDefinition(String beanName, Class<?> type) {
		BindMethod bindMethod = ConfigurationPropertiesBean.deduceBindMethod(type);
		RootBeanDefinition definition = new RootBeanDefinition(type);
		BindMethodAttribute.set(definition, bindMethod);
		if (bindMethod == BindMethod.VALUE_OBJECT) {
			definition.setInstanceSupplier(() -> ConstructorBound.from(this.beanFactory, beanName, type));
		}
		return definition;
	}

}
