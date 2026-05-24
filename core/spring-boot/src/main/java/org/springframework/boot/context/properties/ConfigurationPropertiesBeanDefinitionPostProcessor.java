/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanDefinitionRegistryPostProcessor} that ensures any bean registered
 * programmatically (for example, via {@code BeanRegistrar}) that is annotated with
 * {@link ConfigurationProperties @ConfigurationProperties} is enriched with the correct
 * {@link BindMethodAttribute} and, for constructor-bound types, an
 * {@code instanceSupplier}. Enrichment is delegated to
 * {@link ConfigurationPropertiesBeanRegistrar#enrichBeanDefinition}.
 *
 * @author Ujjawal Tyagi
 * @since 4.0.0
 */
final class ConfigurationPropertiesBeanDefinitionPostProcessor
		implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

	static final String BEAN_NAME = ConfigurationPropertiesBeanDefinitionPostProcessor.class.getName();

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		ConfigurableListableBeanFactory beanFactory = (registry instanceof ConfigurableListableBeanFactory clbf)
				? clbf : null;
		@Nullable ClassLoader classLoader = (beanFactory != null) ? beanFactory.getBeanClassLoader()
				: ClassUtils.getDefaultClassLoader();
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition definition = registry.getBeanDefinition(beanName);
			if (BindMethodAttribute.get(definition) != null) {
				continue; // already enriched (e.g. via @EnableConfigurationProperties or scan)
			}
			Class<?> type = resolveClass(definition, classLoader);
			if (type == null) {
				continue;
			}
			if (!MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY)
					.isPresent(ConfigurationProperties.class)) {
				continue;
			}
			if (beanFactory != null) {
				ConfigurationPropertiesBeanRegistrar.enrichBeanDefinition(beanName, definition, type, beanFactory);
			}
		}
	}

	private @Nullable Class<?> resolveClass(BeanDefinition definition, @Nullable ClassLoader classLoader) {
		if (definition instanceof AbstractBeanDefinition abstractDef && abstractDef.hasBeanClass()) {
			return abstractDef.getBeanClass();
		}
		String className = definition.getBeanClassName();
		if (className != null) {
			try {
				return ClassUtils.forName(className, classLoader);
			}
			catch (Throwable ex) {
				// Ignore
			}
		}
		return null;
	}

}
