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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link ServletComponentScan @ServletComponentScan}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class ServletComponentScanRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String BEAN_NAME = "servletComponentRegisteringPostProcessor";

	/**
	 * Register the bean definitions for the ServletComponentScanRegistrar. This method is
	 * responsible for scanning the specified packages and adding the necessary post
	 * processors to the bean definition registry.
	 * @param importingClassMetadata the metadata of the importing class
	 * @param registry the bean definition registry
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
		if (registry.containsBeanDefinition(BEAN_NAME)) {
			updatePostProcessor(registry, packagesToScan);
		}
		else {
			addPostProcessor(registry, packagesToScan);
		}
	}

	/**
	 * Updates the post processor for scanning servlet components.
	 * @param registry the bean definition registry
	 * @param packagesToScan the set of package names to scan for servlet components
	 */
	private void updatePostProcessor(BeanDefinitionRegistry registry, Set<String> packagesToScan) {
		ServletComponentRegisteringPostProcessorBeanDefinition definition = (ServletComponentRegisteringPostProcessorBeanDefinition) registry
			.getBeanDefinition(BEAN_NAME);
		definition.addPackageNames(packagesToScan);
	}

	/**
	 * Adds a post processor to the given bean definition registry for scanning the
	 * specified packages. This post processor is responsible for registering servlet
	 * components found in the packages.
	 * @param registry the bean definition registry to add the post processor to
	 * @param packagesToScan the set of packages to scan for servlet components
	 */
	private void addPostProcessor(BeanDefinitionRegistry registry, Set<String> packagesToScan) {
		ServletComponentRegisteringPostProcessorBeanDefinition definition = new ServletComponentRegisteringPostProcessorBeanDefinition(
				packagesToScan);
		registry.registerBeanDefinition(BEAN_NAME, definition);
	}

	/**
	 * Retrieves the packages to scan based on the provided annotation metadata.
	 * @param metadata the annotation metadata
	 * @return a set of packages to scan
	 */
	private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes
			.fromMap(metadata.getAnnotationAttributes(ServletComponentScan.class.getName()));
		String[] basePackages = attributes.getStringArray("basePackages");
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
		Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
		for (Class<?> basePackageClass : basePackageClasses) {
			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
		}
		if (packagesToScan.isEmpty()) {
			packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
		}
		return packagesToScan;
	}

	/**
	 * ServletComponentRegisteringPostProcessorBeanDefinition class.
	 */
	static final class ServletComponentRegisteringPostProcessorBeanDefinition extends GenericBeanDefinition {

		private final Set<String> packageNames = new LinkedHashSet<>();

		/**
		 * Sets the bean class to ServletComponentRegisteringPostProcessor and sets the
		 * role to BeanDefinition.ROLE_INFRASTRUCTURE. Adds the given package names to the
		 * list of package names.
		 * @param packageNames the collection of package names to be added
		 */
		ServletComponentRegisteringPostProcessorBeanDefinition(Collection<String> packageNames) {
			setBeanClass(ServletComponentRegisteringPostProcessor.class);
			setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			addPackageNames(packageNames);
		}

		/**
		 * Returns a supplier that provides an instance of
		 * ServletComponentRegisteringPostProcessor. The supplier creates a new instance
		 * of ServletComponentRegisteringPostProcessor with the specified package names.
		 * @return a supplier that provides an instance of
		 * ServletComponentRegisteringPostProcessor
		 */
		@Override
		public Supplier<?> getInstanceSupplier() {
			return () -> new ServletComponentRegisteringPostProcessor(this.packageNames);
		}

		/**
		 * Adds additional package names to the existing collection of package names.
		 * @param additionalPackageNames the collection of package names to be added
		 */
		private void addPackageNames(Collection<String> additionalPackageNames) {
			this.packageNames.addAll(additionalPackageNames);
		}

	}

}
