/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
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
 */
class EnableConfigurationPropertiesImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				EnableConfigurationProperties.class.getName(), false);
		Object[] type = attributes == null ? null : (Object[]) attributes
				.getFirst("value");
		if (type == null || type.length == 0) {
			return new String[] { ConfigurationPropertiesBindingPostProcessorRegistrar.class
					.getName() };
		}
		return new String[] { ConfigurationPropertiesBeanRegistrar.class.getName(),
				ConfigurationPropertiesBindingPostProcessorRegistrar.class.getName() };
	}

	public static class ConfigurationPropertiesBeanRegistrar implements
			ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata,
				BeanDefinitionRegistry registry) {
			MultiValueMap<String, Object> attributes = metadata
					.getAllAnnotationAttributes(
							EnableConfigurationProperties.class.getName(), false);
			List<Class<?>> types = collectClasses(attributes.get("value"));
			for (Class<?> type : types) {
				String prefix = extractPrefix(type);
				String name = (StringUtils.hasText(prefix) ? prefix
						+ ".CONFIGURATION_PROPERTIES" : type.getName());
				if (!registry.containsBeanDefinition(name)) {
					registerBeanDefinition(registry, type, name);
				}
			}
		}

		private String extractPrefix(Class<?> type) {
			ConfigurationProperties annotation = AnnotationUtils.findAnnotation(type,
					ConfigurationProperties.class);
			if (annotation != null) {
				return (StringUtils.hasLength(annotation.value()) ? annotation.value()
						: annotation.prefix());
			}
			return "";
		}

		private List<Class<?>> collectClasses(List<Object> list) {
			ArrayList<Class<?>> result = new ArrayList<Class<?>>();
			for (Object object : list) {
				for (Object value : (Object[]) object) {
					if (value instanceof Class && value != void.class) {
						result.add((Class<?>) value);
					}
				}
			}
			return result;
		}

		private void registerBeanDefinition(BeanDefinitionRegistry registry,
				Class<?> type, String name) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(type);
			AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
			registry.registerBeanDefinition(name, beanDefinition);

			ConfigurationProperties properties = AnnotationUtils.findAnnotation(type,
					ConfigurationProperties.class);
			if (properties == null) {
				registerPropertiesHolder(registry, name);
			}
		}

		private void registerPropertiesHolder(BeanDefinitionRegistry registry, String name) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurationPropertiesHolder.class);
			builder.addConstructorArgReference(name);
			registry.registerBeanDefinition(name + ".HOLDER", builder.getBeanDefinition());
		}

	}

}
