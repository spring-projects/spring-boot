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

package org.springframework.boot.context.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Import selector that sets up binding of external properties to configuration classes
 * (see {@link ConfigurationProperties}). It either registers a
 * {@link ConfigurationProperties} bean or not, depending on whether the enclosing
 * {@link EnableConfigurationProperties} explicitly declares one. If none is declared then
 * a bean post processor will still kick in for any beans annotated as external
 * configuration. If one is declared then it a bean definition is registered with id equal
 * to the class name (thus an application context usually only contains one
 * {@link ConfigurationProperties} bean of each unique type).
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
class EnableConfigurationPropertiesImportSelector implements ImportSelector {

	private static final String[] IMPORTS = {
			ConfigurationPropertiesBeanRegistrar.class.getName(),
			ConfigurationPropertiesBindingPostProcessorRegistrar.class.getName() };

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		return IMPORTS;
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} for configuration properties support.
	 */
	public static class ConfigurationPropertiesBeanRegistrar
			implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata,
				BeanDefinitionRegistry registry) {
			getTypes(metadata).forEach((type) -> register(registry,
					(ConfigurableListableBeanFactory) registry, type));
		}

		private List<Class<?>> getTypes(AnnotationMetadata metadata) {
			MultiValueMap<String, Object> attributes = metadata
					.getAllAnnotationAttributes(
							EnableConfigurationProperties.class.getName(), false);
			return collectClasses(attributes == null ? Collections.emptyList()
					: attributes.get("value"));
		}

		private List<Class<?>> collectClasses(List<?> values) {
			return values.stream().flatMap((value) -> Arrays.stream((Object[]) value))
					.map((o) -> (Class<?>) o).filter((type) -> void.class != type)
					.collect(Collectors.toList());
		}

		private void register(BeanDefinitionRegistry registry,
				ConfigurableListableBeanFactory beanFactory, Class<?> type) {
			String name = getName(type);
			if (!containsBeanDefinition(beanFactory, name)) {
				registerBeanDefinition(registry, name, type);
			}
		}

		private String getName(Class<?> type) {
			ConfigurationProperties annotation = AnnotationUtils.findAnnotation(type,
					ConfigurationProperties.class);
			String prefix = (annotation != null ? annotation.prefix() : "");
			return (StringUtils.hasText(prefix) ? prefix + "-" + type.getName()
					: type.getName());
		}

		private boolean containsBeanDefinition(
				ConfigurableListableBeanFactory beanFactory, String name) {
			if (beanFactory.containsBeanDefinition(name)) {
				return true;
			}
			BeanFactory parent = beanFactory.getParentBeanFactory();
			if (parent instanceof ConfigurableListableBeanFactory) {
				return containsBeanDefinition((ConfigurableListableBeanFactory) parent,
						name);
			}
			return false;
		}

		private void registerBeanDefinition(BeanDefinitionRegistry registry, String name,
				Class<?> type) {
			assertHasAnnotation(type);
			GenericBeanDefinition definition = new GenericBeanDefinition();
			definition.setBeanClass(type);
			registry.registerBeanDefinition(name, definition);
		}

		private void assertHasAnnotation(Class<?> type) {
			Assert.notNull(
					AnnotationUtils.findAnnotation(type, ConfigurationProperties.class),
					"No " + ConfigurationProperties.class.getSimpleName()
							+ " annotation found on  '" + type.getName() + "'.");
		}

	}

}
