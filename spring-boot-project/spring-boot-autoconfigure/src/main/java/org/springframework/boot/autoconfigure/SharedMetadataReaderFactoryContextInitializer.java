/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.type.classreading.ConcurrentReferenceCachingMetadataReaderFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * {@link ApplicationContextInitializer} to create a shared
 * {@link CachingMetadataReaderFactory} between the
 * {@link ConfigurationClassPostProcessor} and Spring Boot.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
class SharedMetadataReaderFactoryContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	public static final String BEAN_NAME = "org.springframework.boot.autoconfigure."
			+ "internalCachingMetadataReaderFactory";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addBeanFactoryPostProcessor(
				new CachingMetadataReaderFactoryPostProcessor());
	}

	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} to register the
	 * {@link CachingMetadataReaderFactory} and configure the
	 * {@link ConfigurationClassPostProcessor}.
	 */
	private static class CachingMetadataReaderFactoryPostProcessor
			implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

		@Override
		public int getOrder() {
			// Must happen before the ConfigurationClassPostProcessor is created
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			register(registry);
			configureConfigurationClassPostProcessor(registry);
		}

		private void register(BeanDefinitionRegistry registry) {
			BeanDefinition definition = BeanDefinitionBuilder
					.genericBeanDefinition(SharedMetadataReaderFactoryBean.class,
							SharedMetadataReaderFactoryBean::new)
					.getBeanDefinition();
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}

		private void configureConfigurationClassPostProcessor(
				BeanDefinitionRegistry registry) {
			try {
				BeanDefinition definition = registry.getBeanDefinition(
						AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME);
				definition.getPropertyValues().add("metadataReaderFactory",
						new RuntimeBeanReference(BEAN_NAME));
			}
			catch (NoSuchBeanDefinitionException ex) {
			}
		}

	}

	/**
	 * {@link FactoryBean} to create the shared {@link MetadataReaderFactory}.
	 */
	static class SharedMetadataReaderFactoryBean
			implements FactoryBean<ConcurrentReferenceCachingMetadataReaderFactory>,
			BeanClassLoaderAware, ApplicationListener<ContextRefreshedEvent> {

		private ConcurrentReferenceCachingMetadataReaderFactory metadataReaderFactory;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.metadataReaderFactory = new ConcurrentReferenceCachingMetadataReaderFactory(
					classLoader);
		}

		@Override
		public ConcurrentReferenceCachingMetadataReaderFactory getObject()
				throws Exception {
			return this.metadataReaderFactory;
		}

		@Override
		public Class<?> getObjectType() {
			return CachingMetadataReaderFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.metadataReaderFactory.clearCache();
		}

	}

}
