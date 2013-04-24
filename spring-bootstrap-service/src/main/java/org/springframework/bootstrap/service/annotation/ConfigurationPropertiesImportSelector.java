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

package org.springframework.bootstrap.service.annotation;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.bootstrap.context.annotation.ConfigurationProperties;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.MultiValueMap;

/**
 * Import selector that sets up binding of external properties to configuration classes (see
 * {@link ConfigurationProperties}). It either registers a {@link ConfigurationProperties} bean or not, depending on
 * whether the enclosing {@link EnableConfigurationProperties} explicitly declares one. If none is declared then a bean
 * post processor will still kick in for any beans annotated as external configuration. If one is declared then it a
 * bean definition is registered with id equal to the class name (thus an application context usually only contains one
 * {@link ConfigurationProperties} bean of each unique type).
 * 
 * @author Dave Syer
 */
public class ConfigurationPropertiesImportSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				EnableConfigurationProperties.class.getName(), false);
		Object type = attributes.getFirst("value");
		if (type == void.class) {
			return new String[] { ConfigurationPropertiesBindingConfiguration.class.getName() };
		}
		return new String[] { ConfigurationPropertiesBeanRegistrar.class.getName(),
				ConfigurationPropertiesBindingConfiguration.class.getName() };
	}

	public static class ConfigurationPropertiesBeanRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
			MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
					EnableConfigurationProperties.class.getName(), false);
			Class<?> type = (Class<?>) attributes.getFirst("value");
			registry.registerBeanDefinition(type.getName(), BeanDefinitionBuilder.genericBeanDefinition(type)
					.getBeanDefinition());
		}

	}

}
