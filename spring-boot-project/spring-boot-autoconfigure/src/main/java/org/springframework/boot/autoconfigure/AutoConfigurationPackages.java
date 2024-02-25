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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Class for storing auto-configuration packages for reference later (e.g. by JPA entity
 * scanner).
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 * @since 1.0.0
 */
public abstract class AutoConfigurationPackages {

	private static final Log logger = LogFactory.getLog(AutoConfigurationPackages.class);

	private static final String BEAN = AutoConfigurationPackages.class.getName();

	/**
	 * Determine if the auto-configuration base packages for the given bean factory are
	 * available.
	 * @param beanFactory the source bean factory
	 * @return true if there are auto-config packages available
	 */
	public static boolean has(BeanFactory beanFactory) {
		return beanFactory.containsBean(BEAN) && !get(beanFactory).isEmpty();
	}

	/**
	 * Return the auto-configuration base packages for the given bean factory.
	 * @param beanFactory the source bean factory
	 * @return a list of auto-configuration packages
	 * @throws IllegalStateException if auto-configuration is not enabled
	 */
	public static List<String> get(BeanFactory beanFactory) {
		try {
			return beanFactory.getBean(BEAN, BasePackages.class).get();
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new IllegalStateException("Unable to retrieve @EnableAutoConfiguration base packages");
		}
	}

	/**
	 * Programmatically registers the auto-configuration package names. Subsequent
	 * invocations will add the given package names to those that have already been
	 * registered. You can use this method to manually define the base packages that will
	 * be used for a given {@link BeanDefinitionRegistry}. Generally it's recommended that
	 * you don't call this method directly, but instead rely on the default convention
	 * where the package name is set from your {@code @EnableAutoConfiguration}
	 * configuration class or classes.
	 * @param registry the bean definition registry
	 * @param packageNames the package names to set
	 */
	public static void register(BeanDefinitionRegistry registry, String... packageNames) {
		if (registry.containsBeanDefinition(BEAN)) {
			addBasePackages(registry.getBeanDefinition(BEAN), packageNames);
		}
		else {
			RootBeanDefinition beanDefinition = new RootBeanDefinition(BasePackages.class);
			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			addBasePackages(beanDefinition, packageNames);
			registry.registerBeanDefinition(BEAN, beanDefinition);
		}
	}

	/**
	 * Adds additional base packages to the existing base packages in the given
	 * BeanDefinition.
	 * @param beanDefinition The BeanDefinition to add the base packages to.
	 * @param additionalBasePackages The additional base packages to be added.
	 */
	private static void addBasePackages(BeanDefinition beanDefinition, String[] additionalBasePackages) {
		ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
		if (constructorArgumentValues.hasIndexedArgumentValue(0)) {
			String[] existingPackages = (String[]) constructorArgumentValues.getIndexedArgumentValue(0, String[].class)
				.getValue();
			constructorArgumentValues.addIndexedArgumentValue(0,
					Stream.concat(Stream.of(existingPackages), Stream.of(additionalBasePackages))
						.distinct()
						.toArray(String[]::new));
		}
		else {
			constructorArgumentValues.addIndexedArgumentValue(0, additionalBasePackages);
		}
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
	 * configuration.
	 */
	static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {

		/**
		 * Register bean definitions based on the provided annotation metadata and bean
		 * definition registry.
		 * @param metadata the annotation metadata
		 * @param registry the bean definition registry
		 */
		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
			register(registry, new PackageImports(metadata).getPackageNames().toArray(new String[0]));
		}

		/**
		 * Determines the imports required for the given annotation metadata.
		 * @param metadata the annotation metadata to determine imports for
		 * @return a set of objects representing the required imports
		 */
		@Override
		public Set<Object> determineImports(AnnotationMetadata metadata) {
			return Collections.singleton(new PackageImports(metadata));
		}

	}

	/**
	 * Wrapper for a package import.
	 */
	private static final class PackageImports {

		private final List<String> packageNames;

		/**
		 * Constructs a new PackageImports object by extracting the base package names
		 * from the provided AnnotationMetadata.
		 * @param metadata the AnnotationMetadata object containing the annotation
		 * attributes
		 */
		PackageImports(AnnotationMetadata metadata) {
			AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(AutoConfigurationPackage.class.getName(), false));
			List<String> packageNames = new ArrayList<>(Arrays.asList(attributes.getStringArray("basePackages")));
			for (Class<?> basePackageClass : attributes.getClassArray("basePackageClasses")) {
				packageNames.add(basePackageClass.getPackage().getName());
			}
			if (packageNames.isEmpty()) {
				packageNames.add(ClassUtils.getPackageName(metadata.getClassName()));
			}
			this.packageNames = Collections.unmodifiableList(packageNames);
		}

		/**
		 * Returns a list of package names.
		 * @return the list of package names
		 */
		List<String> getPackageNames() {
			return this.packageNames;
		}

		/**
		 * Compares this PackageImports object to the specified object for equality.
		 * @param obj the object to compare to
		 * @return true if the specified object is equal to this PackageImports object,
		 * false otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.packageNames.equals(((PackageImports) obj).packageNames);
		}

		/**
		 * Returns the hash code value for the object. This method overrides the
		 * hashCode() method defined in the Object class.
		 * @return the hash code value for the object.
		 */
		@Override
		public int hashCode() {
			return this.packageNames.hashCode();
		}

		/**
		 * Returns a string representation of the PackageImports object.
		 * @return a string representation of the PackageImports object
		 */
		@Override
		public String toString() {
			return "Package Imports " + this.packageNames;
		}

	}

	/**
	 * Holder for the base package (name may be null to indicate no scanning).
	 */
	static final class BasePackages {

		private final List<String> packages;

		private boolean loggedBasePackageInfo;

		/**
		 * Constructs a new BasePackages object with the provided package names.
		 * @param names the package names to be added
		 */
		BasePackages(String... names) {
			List<String> packages = new ArrayList<>();
			for (String name : names) {
				if (StringUtils.hasText(name)) {
					packages.add(name);
				}
			}
			this.packages = packages;
		}

		/**
		 * Retrieves the list of packages.
		 * @return The list of packages.
		 */
		List<String> get() {
			if (!this.loggedBasePackageInfo) {
				if (this.packages.isEmpty()) {
					if (logger.isWarnEnabled()) {
						logger.warn("@EnableAutoConfiguration was declared on a class "
								+ "in the default package. Automatic @Repository and "
								+ "@Entity scanning is not enabled.");
					}
				}
				else {
					if (logger.isDebugEnabled()) {
						String packageNames = StringUtils.collectionToCommaDelimitedString(this.packages);
						logger.debug("@EnableAutoConfiguration was declared on a class in the package '" + packageNames
								+ "'. Automatic @Repository and @Entity scanning is enabled.");
					}
				}
				this.loggedBasePackageInfo = true;
			}
			return this.packages;
		}

	}

}
