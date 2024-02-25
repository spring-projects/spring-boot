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

	/**
	 * Initializes the application context by adding a bean factory post processor for
	 * caching metadata reader factory.
	 * @param applicationContext the configurable application context
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (AotDetector.useGeneratedArtifacts()) {
			return;
		}
		BeanFactoryPostProcessor postProcessor = new CachingMetadataReaderFactoryPostProcessor(applicationContext);
		applicationContext.addBeanFactoryPostProcessor(postProcessor);
	}

	/**
	 * Returns the order value of this context initializer.
	 * @return the order value
	 */
	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * Determines if the given registered bean is excluded from Ahead-of-Time (AOT)
	 * processing.
	 * @param registeredBean the registered bean to check
	 * @return {@code true} if the bean is excluded from AOT processing, {@code false}
	 * otherwise
	 */
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

		/**
		 * Constructs a new CachingMetadataReaderFactoryPostProcessor with the specified
		 * ConfigurableApplicationContext.
		 * @param context the ConfigurableApplicationContext to be used by the
		 * CachingMetadataReaderFactoryPostProcessor
		 */
		CachingMetadataReaderFactoryPostProcessor(ConfigurableApplicationContext context) {
			this.context = context;
		}

		/**
		 * Returns the order in which this post-processor should be executed. This method
		 * must be called before the ConfigurationClassPostProcessor is created.
		 * @return the order value indicating the precedence of this post-processor
		 */
		@Override
		public int getOrder() {
			// Must happen before the ConfigurationClassPostProcessor is created
			return Ordered.HIGHEST_PRECEDENCE;
		}

		/**
		 * {@inheritDoc}
		 *
		 * This method is called after the bean factory has been initialized and allows
		 * for post-processing of the bean factory.
		 * @param beanFactory the bean factory that has been initialized
		 * @throws BeansException if any error occurs during post-processing
		 */
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		/**
		 * Post-processes the bean definition registry.
		 * @param registry the bean definition registry to be processed
		 * @throws BeansException if any error occurs during the processing
		 */
		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			register(registry);
			configureConfigurationClassPostProcessor(registry);
		}

		/**
		 * Registers a bean definition for the SharedMetadataReaderFactoryBean if it does
		 * not already exist in the given registry.
		 * @param registry the BeanDefinitionRegistry to register the bean definition with
		 */
		private void register(BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(BEAN_NAME)) {
				BeanDefinition definition = BeanDefinitionBuilder
					.rootBeanDefinition(SharedMetadataReaderFactoryBean.class, SharedMetadataReaderFactoryBean::new)
					.getBeanDefinition();
				registry.registerBeanDefinition(BEAN_NAME, definition);
			}
		}

		/**
		 * Configures the ConfigurationClassPostProcessor for the given
		 * BeanDefinitionRegistry.
		 * @param registry the BeanDefinitionRegistry to configure
		 */
		private void configureConfigurationClassPostProcessor(BeanDefinitionRegistry registry) {
			try {
				configureConfigurationClassPostProcessor(
						registry.getBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}

		/**
		 * Configures the ConfigurationClassPostProcessor for the given BeanDefinition.
		 * @param definition the BeanDefinition to configure
		 */
		private void configureConfigurationClassPostProcessor(BeanDefinition definition) {
			if (definition instanceof AbstractBeanDefinition abstractBeanDefinition) {
				configureConfigurationClassPostProcessor(abstractBeanDefinition);
				return;
			}
			configureConfigurationClassPostProcessor(definition.getPropertyValues());
		}

		/**
		 * Configures the ConfigurationClassPostProcessor for the given bean definition.
		 * If the bean definition has an instance supplier, it sets a customizing supplier
		 * that wraps the original supplier with a
		 * ConfigurationClassPostProcessorCustomizingSupplier. Otherwise, it configures
		 * the ConfigurationClassPostProcessor with the given property values.
		 * @param definition the bean definition to configure
		 */
		private void configureConfigurationClassPostProcessor(AbstractBeanDefinition definition) {
			Supplier<?> instanceSupplier = definition.getInstanceSupplier();
			if (instanceSupplier != null) {
				definition.setInstanceSupplier(
						new ConfigurationClassPostProcessorCustomizingSupplier(this.context, instanceSupplier));
				return;
			}
			configureConfigurationClassPostProcessor(definition.getPropertyValues());
		}

		/**
		 * Configures the ConfigurationClassPostProcessor by adding the
		 * metadataReaderFactory property.
		 * @param propertyValues the MutablePropertyValues object containing the property
		 * values
		 */
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

		/**
		 * Constructs a new ConfigurationClassPostProcessorCustomizingSupplier with the
		 * specified ConfigurableApplicationContext and instanceSupplier.
		 * @param context the ConfigurableApplicationContext to be used
		 * @param instanceSupplier the Supplier to be used for supplying instances
		 */
		ConfigurationClassPostProcessorCustomizingSupplier(ConfigurableApplicationContext context,
				Supplier<?> instanceSupplier) {
			this.context = context;
			this.instanceSupplier = instanceSupplier;
		}

		/**
		 * Retrieves an instance of an object.
		 * @return The retrieved object instance.
		 */
		@Override
		public Object get() {
			Object instance = this.instanceSupplier.get();
			if (instance instanceof ConfigurationClassPostProcessor postProcessor) {
				configureConfigurationClassPostProcessor(postProcessor);
			}
			return instance;
		}

		/**
		 * Configures the ConfigurationClassPostProcessor instance by setting the
		 * MetadataReaderFactory.
		 * @param instance the ConfigurationClassPostProcessor instance to be configured
		 */
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

		/**
		 * Set the class loader to use for loading bean classes.
		 * @param classLoader the class loader to use
		 */
		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.metadataReaderFactory = new ConcurrentReferenceCachingMetadataReaderFactory(classLoader);
		}

		/**
		 * Retrieves the shared instance of the
		 * ConcurrentReferenceCachingMetadataReaderFactory.
		 * @return The shared instance of the
		 * ConcurrentReferenceCachingMetadataReaderFactory.
		 * @throws Exception if an error occurs while retrieving the shared instance.
		 */
		@Override
		public ConcurrentReferenceCachingMetadataReaderFactory getObject() throws Exception {
			return this.metadataReaderFactory;
		}

		/**
		 * Returns the type of object that is created by this factory bean.
		 * @return the type of object created by this factory bean, which is
		 * {@link CachingMetadataReaderFactory}
		 */
		@Override
		public Class<?> getObjectType() {
			return CachingMetadataReaderFactory.class;
		}

		/**
		 * Returns a boolean value indicating whether the SharedMetadataReaderFactoryBean
		 * is a singleton.
		 * @return true if the SharedMetadataReaderFactoryBean is a singleton, false
		 * otherwise
		 */
		@Override
		public boolean isSingleton() {
			return true;
		}

		/**
		 * Clears the cache of the metadata reader factory when the application context is
		 * refreshed.
		 * @param event the context refreshed event
		 */
		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.metadataReaderFactory.clearCache();
		}

	}

}
