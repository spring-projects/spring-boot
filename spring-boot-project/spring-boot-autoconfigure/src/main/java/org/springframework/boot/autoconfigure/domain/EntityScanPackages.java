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

package org.springframework.boot.autoconfigure.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Class for storing {@link EntityScan @EntityScan} specified packages for reference later
 * (e.g. by JPA auto-configuration).
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see EntityScan
 * @see EntityScanner
 */
public class EntityScanPackages {

	private static final String BEAN = EntityScanPackages.class.getName();

	private static final EntityScanPackages NONE = new EntityScanPackages();

	private final List<String> packageNames;

	EntityScanPackages(String... packageNames) {
		List<String> packages = new ArrayList<>();
		for (String name : packageNames) {
			if (StringUtils.hasText(name)) {
				packages.add(name);
			}
		}
		this.packageNames = Collections.unmodifiableList(packages);
	}

	/**
	 * Return the package names specified from all {@link EntityScan @EntityScan}
	 * annotations.
	 * @return the entity scan package names
	 */
	public List<String> getPackageNames() {
		return this.packageNames;
	}

	/**
	 * Return the {@link EntityScanPackages} for the given bean factory.
	 * @param beanFactory the source bean factory
	 * @return the {@link EntityScanPackages} for the bean factory (never {@code null})
	 */
	public static EntityScanPackages get(BeanFactory beanFactory) {
		// Currently we only store a single base package, but we return a list to
		// allow this to change in the future if needed
		try {
			return beanFactory.getBean(BEAN, EntityScanPackages.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return NONE;
		}
	}

	/**
	 * Register the specified entity scan packages with the system.
	 * @param registry the source registry
	 * @param packageNames the package names to register
	 */
	public static void register(BeanDefinitionRegistry registry, String... packageNames) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(packageNames, "PackageNames must not be null");
		register(registry, Arrays.asList(packageNames));
	}

	/**
	 * Register the specified entity scan packages with the system.
	 * @param registry the source registry
	 * @param packageNames the package names to register
	 */
	public static void register(BeanDefinitionRegistry registry, Collection<String> packageNames) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notNull(packageNames, "PackageNames must not be null");
		if (registry.containsBeanDefinition(BEAN)) {
			EntityScanPackagesBeanDefinition beanDefinition = (EntityScanPackagesBeanDefinition) registry
				.getBeanDefinition(BEAN);
			beanDefinition.addPackageNames(packageNames);
		}
		else {
			registry.registerBeanDefinition(BEAN, new EntityScanPackagesBeanDefinition(packageNames));
		}
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
	 * configuration.
	 */
	static class Registrar implements ImportBeanDefinitionRegistrar {

		private final Environment environment;

		Registrar(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
			register(registry, getPackagesToScan(metadata));
		}

		private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
			AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(EntityScan.class.getName()));
			Set<String> packagesToScan = new LinkedHashSet<>();
			for (String basePackage : attributes.getStringArray("basePackages")) {
				String[] tokenized = StringUtils.tokenizeToStringArray(
						this.environment.resolvePlaceholders(basePackage),
						ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
				Collections.addAll(packagesToScan, tokenized);
			}
			for (Class<?> basePackageClass : attributes.getClassArray("basePackageClasses")) {
				packagesToScan.add(this.environment.resolvePlaceholders(ClassUtils.getPackageName(basePackageClass)));
			}
			if (packagesToScan.isEmpty()) {
				String packageName = ClassUtils.getPackageName(metadata.getClassName());
				Assert.state(StringUtils.hasLength(packageName), "@EntityScan cannot be used with the default package");
				return Collections.singleton(packageName);
			}
			return packagesToScan;
		}

	}

	static class EntityScanPackagesBeanDefinition extends GenericBeanDefinition {

		private final Set<String> packageNames = new LinkedHashSet<>();

		EntityScanPackagesBeanDefinition(Collection<String> packageNames) {
			setBeanClass(EntityScanPackages.class);
			setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			addPackageNames(packageNames);
		}

		private void addPackageNames(Collection<String> additionalPackageNames) {
			this.packageNames.addAll(additionalPackageNames);
			getConstructorArgumentValues().addIndexedArgumentValue(0, StringUtils.toStringArray(this.packageNames));
		}

	}

}
