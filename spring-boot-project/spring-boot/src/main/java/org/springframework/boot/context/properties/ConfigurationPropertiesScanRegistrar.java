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
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for registering {@link ConfigurationProperties}
 * bean definitions via scanning.
 *
 * @author Madhura Bhave
 */
class ConfigurationPropertiesScanRegistrar
		implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

	private Environment environment;

	private ResourceLoader resourceLoader;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
		register(registry, (ConfigurableListableBeanFactory) registry, packagesToScan);
	}

	private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(ConfigurationPropertiesScan.class.getName()));
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

	protected void register(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory,
			Set<String> packagesToScan) {
		scan(packagesToScan, beanFactory, registry);
	}

	protected void scan(Set<String> packages, ConfigurableListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.setEnvironment(this.environment);
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
		for (String basePackage : packages) {
			if (StringUtils.hasText(basePackage)) {
				scan(beanFactory, registry, scanner, basePackage);
			}
		}
	}

	private void scan(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			ClassPathScanningCandidateComponentProvider scanner, String basePackage) throws LinkageError {
		for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
			String beanClassName = candidate.getBeanClassName();
			try {
				Class<?> type = ClassUtils.forName(beanClassName, null);
				validateScanConfiguration(type);
				ConfigurationPropertiesBeanDefinitionRegistrar.register(registry, beanFactory, type);
			}
			catch (ClassNotFoundException ex) {
				// Ignore
			}
		}
	}

	private void validateScanConfiguration(Class<?> type) {
		MergedAnnotation<Component> component = MergedAnnotations
				.from(type, MergedAnnotations.SearchStrategy.EXHAUSTIVE).get(Component.class);
		if (component.isPresent()) {
			throw new InvalidConfigurationPropertiesException(type, component.getRoot().getType());
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
