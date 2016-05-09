/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.scan;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * A base {@link ImportBeanDefinitionRegistrar} used to collect the packages to scan for a
 * given component.
 * <p>
 * Expect to process an annotation type that defines a {@code basePackage} and
 * {@code basePackageClasses} attributes as well as a {@code value} alias of
 * {@code basePackage}.
 * <p>
 * The {@link ImportBeanDefinitionRegistrar} registers a single
 * {@link AbstractEntityScanBeanPostProcessor} implementation with the packages to use.
 *
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 1.4.0
 * @see AbstractEntityScanBeanPostProcessor
 */
public abstract class AbstractEntityScanRegistrar
		implements ImportBeanDefinitionRegistrar {

	private final Class<? extends Annotation> annotationType;

	private final String beanPostProcessorName;

	private final Class<? extends AbstractEntityScanBeanPostProcessor> beanPostProcessorType;

	/**
	 * Create an instance.
	 * @param annotationType the annotation to inspect
	 * @param beanPostProcessorName the name of the bean post processor
	 * @param beanPostProcessorType the type of the bean post processor implementation
	 */
	protected AbstractEntityScanRegistrar(Class<? extends Annotation> annotationType,
			String beanPostProcessorName,
			Class<? extends AbstractEntityScanBeanPostProcessor> beanPostProcessorType) {
		this.beanPostProcessorName = beanPostProcessorName;
		this.annotationType = annotationType;
		this.beanPostProcessorType = beanPostProcessorType;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
		if (!registry.containsBeanDefinition(this.beanPostProcessorName)) {
			addEntityScanBeanPostProcessor(registry, packagesToScan);
		}
		else {
			updateEntityScanBeanPostProcessor(registry, packagesToScan);
		}
	}

	protected Set<String> getPackagesToScan(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(this.annotationType.getName()));
		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray("basePackages");
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
		if (!ObjectUtils.isEmpty(value)) {
			Assert.state(ObjectUtils.isEmpty(basePackages),
					String.format(
							"@%s basePackages and value attributes are mutually exclusive",
							this.annotationType.getSimpleName()));
		}
		Set<String> packagesToScan = new LinkedHashSet<String>();
		packagesToScan.addAll(Arrays.asList(value));
		packagesToScan.addAll(Arrays.asList(basePackages));
		for (Class<?> basePackageClass : basePackageClasses) {
			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
		}
		if (packagesToScan.isEmpty()) {
			return Collections
					.singleton(ClassUtils.getPackageName(metadata.getClassName()));
		}
		return packagesToScan;
	}

	private void addEntityScanBeanPostProcessor(BeanDefinitionRegistry registry,
			Set<String> packagesToScan) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(this.beanPostProcessorType);
		beanDefinition.getConstructorArgumentValues()
				.addGenericArgumentValue(toArray(packagesToScan));
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// We don't need this one to be post processed otherwise it can cause a
		// cascade of bean instantiation that we would rather avoid.
		beanDefinition.setSynthetic(true);
		registry.registerBeanDefinition(this.beanPostProcessorName, beanDefinition);
	}

	private void updateEntityScanBeanPostProcessor(BeanDefinitionRegistry registry,
			Set<String> packagesToScan) {
		BeanDefinition definition = registry
				.getBeanDefinition(this.beanPostProcessorName);
		ConstructorArgumentValues.ValueHolder constructorArguments = definition
				.getConstructorArgumentValues().getGenericArgumentValue(String[].class);
		Set<String> mergedPackages = new LinkedHashSet<String>();
		mergedPackages.addAll(Arrays.asList((String[]) constructorArguments.getValue()));
		mergedPackages.addAll(packagesToScan);
		constructorArguments.setValue(toArray(mergedPackages));
	}

	private String[] toArray(Set<String> set) {
		return set.toArray(new String[set.size()]);
	}

}
