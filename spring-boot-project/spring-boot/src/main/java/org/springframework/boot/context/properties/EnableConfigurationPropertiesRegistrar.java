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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link EnableConfigurationProperties @EnableConfigurationProperties}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME = Conventions
		.getQualifiedAttributeName(EnableConfigurationPropertiesRegistrar.class, "methodValidationExcludeFilter");

	/**
	 * Registers bean definitions for configuration properties.
	 * @param metadata the annotation metadata
	 * @param registry the bean definition registry
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		registerInfrastructureBeans(registry);
		registerMethodValidationExcludeFilter(registry);
		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);
		getTypes(metadata).forEach(beanRegistrar::register);
	}

	/**
	 * Retrieves the types of classes annotated with {@link EnableConfigurationProperties}
	 * from the given {@link AnnotationMetadata}.
	 * @param metadata the {@link AnnotationMetadata} containing the annotations
	 * @return a {@link Set} of {@link Class} objects representing the types of classes
	 * annotated with {@link EnableConfigurationProperties}
	 */
	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
		return metadata.getAnnotations()
			.stream(EnableConfigurationProperties.class)
			.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
			.filter((type) -> void.class != type)
			.collect(Collectors.toSet());
	}

	/**
	 * Registers the infrastructure beans required for configuration properties binding.
	 * @param registry the bean definition registry to register the beans with
	 */
	static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
		ConfigurationPropertiesBindingPostProcessor.register(registry);
		BoundConfigurationProperties.register(registry);
	}

	/**
	 * Registers a method validation exclude filter in the given
	 * {@link BeanDefinitionRegistry}. If the filter is not already registered, a new
	 * {@link BeanDefinition} is created and registered. The filter is created using the
	 * {@link MethodValidationExcludeFilter} class and the "byAnnotation" constructor. The
	 * {@link ConfigurationProperties} class is used as a constructor argument. The role
	 * of the bean definition is set to {@link BeanDefinition.ROLE_INFRASTRUCTURE}.
	 * @param registry the {@link BeanDefinitionRegistry} to register the filter in
	 */
	static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
				.rootBeanDefinition(MethodValidationExcludeFilter.class, "byAnnotation")
				.addConstructorArgValue(ConfigurationProperties.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition();
			registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
		}
	}

}
