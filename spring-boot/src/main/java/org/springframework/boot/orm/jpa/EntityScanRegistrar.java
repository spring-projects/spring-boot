/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.orm.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by {@link EntityScan}.
 *
 * @author Phillip Webb
 */
class EntityScanRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String BEAN_NAME = "entityScanBeanPostProcessor";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
			beanDefinition.setBeanClass(EntityScanBeanPostProcessor.class);
			beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
					getPackagesToScan(importingClassMetadata));
			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// We don't need this one to be post processed otherwise it can cause a
			// cascade of bean instantiation that we would rather avoid.
			beanDefinition.setSynthetic(true);
			registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
		}
	}

	private String[] getPackagesToScan(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(EntityScan.class.getName()));
		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray("basePackages");
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");

		if (!ObjectUtils.isEmpty(value)) {
			Assert.state(ObjectUtils.isEmpty(basePackages),
					"@EntityScan basePackages and value attributes are mutually exclusive");
		}

		Set<String> packagesToScan = new LinkedHashSet<String>();
		packagesToScan.addAll(Arrays.asList(value));
		packagesToScan.addAll(Arrays.asList(basePackages));
		for (Class<?> basePackageClass : basePackageClasses) {
			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
		}
		if (packagesToScan.isEmpty()) {
			return new String[] { ClassUtils.getPackageName(metadata.getClassName()) };
		}
		return new ArrayList<String>(packagesToScan).toArray(new String[packagesToScan
				.size()]);
	}

	/**
	 * {@link BeanPostProcessor} to set
	 * {@link LocalContainerEntityManagerFactoryBean#setPackagesToScan(String...)} based
	 * on an {@link EntityScan} annotation.
	 */
	static class EntityScanBeanPostProcessor implements BeanPostProcessor,
			SmartInitializingSingleton {

		private final String[] packagesToScan;

		private boolean processed;

		public EntityScanBeanPostProcessor(String[] packagesToScan) {
			this.packagesToScan = packagesToScan;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof LocalContainerEntityManagerFactoryBean) {
				LocalContainerEntityManagerFactoryBean factoryBean = (LocalContainerEntityManagerFactoryBean) bean;
				factoryBean.setPackagesToScan(this.packagesToScan);
				this.processed = true;
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		@Override
		public void afterSingletonsInstantiated() {
			Assert.state(this.processed, "Unable to configure "
					+ "LocalContainerEntityManagerFactoryBean from @EntityScan, "
					+ "ensure an appropriate bean is registered.");
		}

	}

}
