/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure.orm.jpa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.bootstrap.autoconfigure.data.JpaRepositoriesAutoConfiguration;
import org.springframework.bootstrap.context.annotation.AutoConfigurationUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Helper to detect a component scan declared in the enclosing context (normally on a
 * {@code @Configuration} class). Once the component scan is detected, the base packages
 * are stored for retrieval later by the {@link JpaRepositoriesAutoConfiguration} .
 * 
 * @author Dave Syer
 */
class JpaComponentScanDetector implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(getClass());

	private BeanFactory beanFactory;

	private MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {
		storeComponentScanBasePackages();
	}

	private void storeComponentScanBasePackages() {
		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			storeComponentScanBasePackages((ConfigurableListableBeanFactory) this.beanFactory);
		} else {
			if (this.logger.isWarnEnabled()) {
				this.logger
						.warn("Unable to read @ComponentScan annotations for auto-configure");
			}
		}
	}

	private void storeComponentScanBasePackages(
			ConfigurableListableBeanFactory beanFactory) {
		List<String> basePackages = new ArrayList<String>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			String[] basePackagesAttribute = (String[]) beanDefinition
					.getAttribute("componentScanBasePackages");
			if (basePackagesAttribute != null) {
				basePackages.addAll(Arrays.asList(basePackagesAttribute));
			}
			AnnotationMetadata metadata = getMetadata(beanDefinition);
			basePackages.addAll(getBasePackages(metadata));
		}
		AutoConfigurationUtils.storeBasePackages(beanFactory, basePackages);
	}

	private AnnotationMetadata getMetadata(BeanDefinition beanDefinition) {
		if (beanDefinition instanceof AbstractBeanDefinition
				&& ((AbstractBeanDefinition) beanDefinition).hasBeanClass()) {
			Class<?> beanClass = ((AbstractBeanDefinition) beanDefinition).getBeanClass();
			if (Enhancer.isEnhanced(beanClass)) {
				beanClass = beanClass.getSuperclass();
			}
			return new StandardAnnotationMetadata(beanClass, true);
		}
		String className = beanDefinition.getBeanClassName();
		if (className != null) {
			try {
				MetadataReader metadataReader = this.metadataReaderFactory
						.getMetadataReader(className);
				return metadataReader.getAnnotationMetadata();
			} catch (IOException ex) {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug(
							"Could not find class file for introspecting @ComponentScan classes: "
									+ className, ex);
				}
			}
		}
		return null;
	}

	private List<String> getBasePackages(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap((metadata == null ? null : metadata.getAnnotationAttributes(
						ComponentScan.class.getName(), true)));
		if (attributes != null) {
			List<String> basePackages = new ArrayList<String>();
			addAllHavingText(basePackages, attributes.getStringArray("value"));
			addAllHavingText(basePackages, attributes.getStringArray("basePackages"));
			for (String packageClass : attributes.getStringArray("basePackageClasses")) {
				basePackages.add(ClassUtils.getPackageName(packageClass));
			}
			if (basePackages.isEmpty()) {
				basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
			}
			return basePackages;
		}
		return Collections.emptyList();
	}

	private void addAllHavingText(List<String> list, String[] strings) {
		for (String s : strings) {
			if (StringUtils.hasText(s)) {
				list.add(s);
			}
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
