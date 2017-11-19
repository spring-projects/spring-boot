/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Encapsulates the task to find the bean name which can be mocked or spied. Offers
 * {@link MockitoBeanNameFinder#getOrGenerateBeanName} for that purpose. Used by
 * {@link MockitoPostProcessor}.
 *
 * @author Andreas Neiser
 */
class MockitoBeanNameFinder {

	private MockitoBeanNameFinder() {
		// only static method calls
	}

	private static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

	private static final BeanNameGenerator BEAN_NAME_GENERATOR = new DefaultBeanNameGenerator();

	static String getOrGenerateBeanName(ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry, Definition definition,
			RootBeanDefinition beanDefinition) {
		Set<String> existingBeans = findCandidateBeans(beanFactory, definition);
		if (existingBeans.isEmpty()) {
			return BEAN_NAME_GENERATOR.generateBeanName(beanDefinition, registry);
		}
		return getBeanName(registry, existingBeans, definition);
	}

	private static String getBeanName(BeanDefinitionRegistry registry,
			Set<String> existingBeanNames, Definition definition) {
		if (StringUtils.hasText(definition.getName())) {
			return definition.getName();
		}
		if (existingBeanNames.size() == 1) {
			return existingBeanNames.iterator().next();
		}
		String beanName = findPrimaryBeanName(registry, existingBeanNames,
				definition.getType());
		if (beanName == null) {
			throw new IllegalStateException("Unable to register bean "
					+ definition.getType()
					+ " expected a single matching/primary bean to replace but found "
					+ existingBeanNames);
		}
		return beanName;
	}

	@Nullable
	private static String findPrimaryBeanName(BeanDefinitionRegistry registry,
			Set<String> existingBeanNames, ResolvableType type) {
		String primaryBeanName = null;
		for (String existingBeanName : existingBeanNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(existingBeanName);
			if (beanDefinition.isPrimary()) {
				if (primaryBeanName != null) {
					throw new NoUniqueBeanDefinitionException(type.resolve(),
							existingBeanNames.size(),
							"more than one 'primary' bean found among candidates: "
									+ existingBeanNames);
				}
				primaryBeanName = existingBeanName;
			}
		}
		return primaryBeanName;
	}

	private static Set<String> findCandidateBeans(
			ConfigurableListableBeanFactory beanFactory, Definition definition) {
		QualifierDefinition qualifier = definition.getQualifier();
		Set<String> candidates = new TreeSet<>();
		for (String candidate : getExistingBeans(beanFactory, definition.getType())) {
			if (qualifier == null || qualifier.matches(beanFactory, candidate)) {
				candidates.add(candidate);
			}
		}
		return candidates;
	}

	private static Set<String> getExistingBeans(
			ConfigurableListableBeanFactory beanFactory, ResolvableType type) {
		Set<String> beans = new LinkedHashSet<>(
				Arrays.asList(beanFactory.getBeanNamesForType(type)));
		String resolvedTypeName = type.resolve(Object.class).getName();
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (resolvedTypeName
					.equals(beanDefinition.getAttribute(FACTORY_BEAN_OBJECT_TYPE))) {
				beans.add(beanName);
			}
		}
		beans.removeIf(ScopedProxyUtils::isScopedTarget);
		return beans;
	}
}
