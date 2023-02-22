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

import java.util.function.Supplier;

import org.springframework.aot.AotDetector;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
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
 * @author Dave Syer
 */
class SharedMetadataReaderFactoryContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered, BeanRegistrationExcludeFilter {

	public static final String BEAN_NAME = "org.springframework.boot.autoconfigure."
			+ "internalCachingMetadataReaderFactory";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (AotDetector.useGeneratedArtifacts()) {
			return;
		}
		BeanFactoryPostProcessor postProcessor = new CachingMetadataReaderFactoryPostProcessor(applicationContext);
		applicationContext.addBeanFactoryPostProcessor(postProcessor);
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
		return BEAN_NAME.equals(registeredBean.getBeanName());
	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} to register the
	 * {@link CachingMetadataReaderFactory} and configure the
	 * {@link ConfigurationClassPostProcessor}.
	 */
	static class CachingMetadataReaderFactoryPostProcessor
			implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

		private final ConfigurableApplicationContext context;

		CachingMetadataReaderFactoryPostProcessor(ConfigurableApplicationContext context) {
			this.context = context;
		}

		@Override
		public int getOrder() {
			// Must happen before the ConfigurationClassPostProcessor is created
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			register(registry);
			configureConfigurationClassPostProcessor(registry);
		}

		private void register(BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(BEAN_NAME)) {
				BeanDefinition definition = BeanDefinitionBuilder
					.rootBeanDefinition(SharedMetadataReaderFactoryBean.class, SharedMetadataReaderFactoryBean::new)
					.getBeanDefinition();
				registry.registerBeanDefinition(BEAN_NAME, definition);
			}
		}

		private void configureConfigurationClassPostProcessor(BeanDefinitionRegistry registry) {
			try {
				configureConfigurationClassPostProcessor(
						registry.getBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
			}
			catch (NoSuchBeanDefinitionException ex) {
			}
		}

		private void configureConfigurationClassPostProcessor(BeanDefinition definition) {
			if (definition instanceof AbstractBeanDefinition abstractBeanDefinition) {
				configureConfigurationClassPostProcessor(abstractBeanDefinition);
				return;
			}
			configureConfigurationClassPostProcessor(definition.getPropertyValues());
		}

		private void configureConfigurationClassPostProcessor(AbstractBeanDefinition definition) {
			Supplier<?> instanceSupplier = definition.getInstanceSupplier();
			if (instanceSupplier != null) {
				definition.setInstanceSupplier(
						new ConfigurationClassPostProcessorCustomizingSupplier(this.context, instanceSupplier));
				return;
			}
			configureConfigurationClassPostProcessor(definition.getPropertyValues());
		}

		private void configureConfigurationClassPostProcessor(MutablePropertyValues propertyValues) {
			propertyValues.add("metadataReaderFactory", new RuntimeBeanReference(BEAN_NAME));
		}

	}

	/**
	 * {@link Supplier} used to customize the {@link ConfigurationClassPostProcessor} when
	 * it's first created.
	 */
	static class ConfigurationClassPostProcessorCustomizingSupplier implements Supplier<Object> {

		private final ConfigurableApplicationContext context;

		private final Supplier<?> instanceSupplier;

		ConfigurationClassPostProcessorCustomizingSupplier(ConfigurableApplicationContext context,
				Supplier<?> instanceSupplier) {
			this.context = context;
			this.instanceSupplier = instanceSupplier;
		}

		@Override
		public Object get() {
			Object instance = this.instanceSupplier.get();
			if (instance instanceof ConfigurationClassPostProcessor postProcessor) {
				configureConfigurationClassPostProcessor(postProcessor);
			}
			return instance;
		}

		private void configureConfigurationClassPostProcessor(ConfigurationClassPostProcessor instance) {
			instance.setMetadataReaderFactory(this.context.getBean(BEAN_NAME, MetadataReaderFactory.class));
		}

	}

	/**
	 * {@link FactoryBean} to create the shared {@link MetadataReaderFactory}.
	 */
	static class SharedMetadataReaderFactoryBean
			implements FactoryBean<ConcurrentReferenceCachingMetadataReaderFactory>, BeanClassLoaderAware,
			ApplicationListener<ContextRefreshedEvent> {

		private ConcurrentReferenceCachingMetadataReaderFactory metadataReaderFactory;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.metadataReaderFactory = new ConcurrentReferenceCachingMetadataReaderFactory(classLoader);
		}

		@Override
		public ConcurrentReferenceCachingMetadataReaderFactory getObject() throws Exception {
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
