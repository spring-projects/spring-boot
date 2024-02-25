/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.data;

import java.lang.annotation.Annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.util.Streamable;

/**
 * Base {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data
 * Repositories.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 * @since 1.0.0
 */
public abstract class AbstractRepositoryConfigurationSourceSupport
		implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {

	private ResourceLoader resourceLoader;

	private BeanFactory beanFactory;

	private Environment environment;

	/**
     * Register bean definitions for repositories.
     * 
     * This method is responsible for registering bean definitions for repositories based on the provided
     * importing class metadata, bean definition registry, and bean name generator.
     * 
     * @param importingClassMetadata the metadata of the class importing the repositories
     * @param registry the bean definition registry to register the repositories with
     * @param importBeanNameGenerator the bean name generator to generate names for the imported beans
     */
    @Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(
				getConfigurationSource(registry, importBeanNameGenerator), this.resourceLoader, this.environment);
		delegate.registerRepositoriesIn(registry, getRepositoryConfigurationExtension());
	}

	/**
     * Register bean definitions based on the given annotation metadata and bean definition registry.
     * 
     * @param importingClassMetadata the annotation metadata of the importing class
     * @param registry the bean definition registry to register the bean definitions with
     */
    @Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		registerBeanDefinitions(importingClassMetadata, registry, null);
	}

	/**
     * Returns the configuration source for the repository.
     * 
     * @param registry the bean definition registry
     * @param importBeanNameGenerator the bean name generator for imported beans
     * @return the configuration source for the repository
     */
    private AnnotationRepositoryConfigurationSource getConfigurationSource(BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(getConfiguration());
		return new AutoConfiguredAnnotationRepositoryConfigurationSource(metadata, getAnnotation(), this.resourceLoader,
				this.environment, registry, importBeanNameGenerator) {
		};
	}

	/**
     * Returns the base packages for auto-configuration.
     * 
     * @return a streamable object containing the base packages
     */
    protected Streamable<String> getBasePackages() {
		return Streamable.of(AutoConfigurationPackages.get(this.beanFactory));
	}

	/**
	 * The Spring Data annotation used to enable the particular repository support.
	 * @return the annotation class
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * The configuration class that will be used by Spring Boot as a template.
	 * @return the configuration class
	 */
	protected abstract Class<?> getConfiguration();

	/**
	 * The {@link RepositoryConfigurationExtension} for the particular repository support.
	 * @return the repository configuration extension
	 */
	protected abstract RepositoryConfigurationExtension getRepositoryConfigurationExtension();

	/**
	 * The {@link BootstrapMode} for the particular repository support. Defaults to
	 * {@link BootstrapMode#DEFAULT}.
	 * @return the bootstrap mode
	 */
	protected BootstrapMode getBootstrapMode() {
		return BootstrapMode.DEFAULT;
	}

	/**
     * Set the resource loader to be used for loading resources.
     * 
     * @param resourceLoader the resource loader to be used
     */
    @Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
     * Set the BeanFactory that this object runs in.
     * <p>
     * Invoked after population of normal bean properties but before an init callback such as InitializingBean's
     * {@code afterPropertiesSet} or a custom init-method. Invoked after ResourceLoaderAware's {@code setResourceLoader},
     * ApplicationEventPublisherAware's {@code setApplicationEventPublisher} and MessageSourceAware's
     * {@code setMessageSource}.
     * <p>
     * Used to resolve dependencies that cannot be resolved via setters, like a ApplicationContext reference or a
     * ResourceLoader reference.
     * <p>
     * This method will be called after all the properties have been set, and before the initialization callback methods
     * are invoked.
     * 
     * @param beanFactory the BeanFactory object that this object runs in
     * @throws BeansException if initialization of the BeanFactory failed
     */
    @Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
     * Set the environment for this configuration source.
     * 
     * @param environment the environment to set
     */
    @Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * An auto-configured {@link AnnotationRepositoryConfigurationSource}.
	 */
	private class AutoConfiguredAnnotationRepositoryConfigurationSource
			extends AnnotationRepositoryConfigurationSource {

		/**
         * Constructs a new AutoConfiguredAnnotationRepositoryConfigurationSource with the specified parameters.
         *
         * @param metadata the metadata of the annotation
         * @param annotation the class of the annotation
         * @param resourceLoader the resource loader to use
         * @param environment the environment to use
         * @param registry the bean definition registry to use
         * @param generator the bean name generator to use
         */
        AutoConfiguredAnnotationRepositoryConfigurationSource(AnnotationMetadata metadata,
				Class<? extends Annotation> annotation, ResourceLoader resourceLoader, Environment environment,
				BeanDefinitionRegistry registry, BeanNameGenerator generator) {
			super(metadata, annotation, resourceLoader, environment, registry, generator);
		}

		/**
         * Returns the base packages for the repository configuration.
         *
         * @return the base packages for the repository configuration
         */
        @Override
		public Streamable<String> getBasePackages() {
			return AbstractRepositoryConfigurationSourceSupport.this.getBasePackages();
		}

		/**
         * Returns the bootstrap mode of the repository configuration source.
         *
         * @return the bootstrap mode of the repository configuration source
         */
        @Override
		public BootstrapMode getBootstrapMode() {
			return AbstractRepositoryConfigurationSourceSupport.this.getBootstrapMode();
		}

	}

}
