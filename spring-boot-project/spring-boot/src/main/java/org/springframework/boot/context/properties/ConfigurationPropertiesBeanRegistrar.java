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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.MultiValueMap;

/**
 * {@link ImportBeanDefinitionRegistrar} for configuration properties support.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesBeanRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
		getTypes(metadata).forEach(
				(type) -> ConfigurationPropertiesBeanDefinitionRegistrar.register(registry, beanFactory, type));
	}

	private List<Class<?>> getTypes(AnnotationMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata
				.getAllAnnotationAttributes(EnableConfigurationProperties.class.getName(), false);
		return collectClasses((attributes != null) ? attributes.get("value") : Collections.emptyList());
	}

	private List<Class<?>> collectClasses(List<?> values) {
		return values.stream().flatMap((value) -> Arrays.stream((Class<?>[]) value))
				.filter((type) -> void.class != type).collect(Collectors.toList());
	}

}
