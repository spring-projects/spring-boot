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

package org.springframework.bootstrap.context.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}.
 * 
 * @author Phillip Webb
 * @see EnableAutoConfiguration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class EnableAutoConfigurationImportSelector implements DeferredImportSelector,
		BeanClassLoaderAware, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(getClass());

	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;

	private MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		storeComponentScanBasePackages();
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(EnableAutoConfiguration.class.getName(), true));
		List<String> factories = new ArrayList<String>(
				SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class,
						this.beanClassLoader));
		factories.removeAll(Arrays.asList(attributes.getStringArray("exclude")));
		return factories.toArray(new String[factories.size()]);
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
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
