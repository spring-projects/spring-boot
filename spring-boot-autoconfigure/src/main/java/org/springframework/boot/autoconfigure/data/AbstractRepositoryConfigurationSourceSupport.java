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

package org.springframework.boot.autoconfigure.data;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryBeanDefinitionBuilder;
import org.springframework.data.repository.config.RepositoryBeanNameGenerator;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Base {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data
 * Repositories.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 */
public abstract class AbstractRepositoryConfigurationSourceSupport implements
		BeanFactoryAware, ImportBeanDefinitionRegistrar, BeanClassLoaderAware,
		EnvironmentAware {

	private static Log logger = LogFactory
			.getLog(AbstractRepositoryConfigurationSourceSupport.class);

	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;

	private Environment environment;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {

		ResourceLoader resourceLoader = new DefaultResourceLoader();
		AnnotationRepositoryConfigurationSource configurationSource = getConfigurationSource();
		RepositoryConfigurationExtension extension = getRepositoryConfigurationExtension();
		extension.registerBeansForRoot(registry, configurationSource);

		RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
		generator.setBeanClassLoader(this.beanClassLoader);

		Collection<RepositoryConfiguration<AnnotationRepositoryConfigurationSource>> repositoryConfigurations = extension
				.getRepositoryConfigurations(configurationSource, resourceLoader);

		for (RepositoryConfiguration<AnnotationRepositoryConfigurationSource> repositoryConfiguration : repositoryConfigurations) {
			RepositoryBeanDefinitionBuilder builder = new RepositoryBeanDefinitionBuilder(
					repositoryConfiguration, extension);
			BeanDefinitionBuilder definitionBuilder = builder.build(registry,
					resourceLoader);
			extension.postProcess(definitionBuilder, configurationSource);

			String beanName = generator.generateBeanName(
					definitionBuilder.getBeanDefinition(), registry);
			registry.registerBeanDefinition(beanName,
					definitionBuilder.getBeanDefinition());
		}
	}

	private AnnotationRepositoryConfigurationSource getConfigurationSource() {
		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				getConfiguration(), true);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
				metadata, getAnnotation(), this.environment) {

			@Override
			public java.lang.Iterable<String> getBasePackages() {
				return AbstractRepositoryConfigurationSourceSupport.this
						.getBasePackages();
			};
		};
		return configurationSource;
	}

	protected Iterable<String> getBasePackages() {
		List<String> basePackages = AutoConfigurationUtils
				.getBasePackages(this.beanFactory);
		if (basePackages.isEmpty()) {
			logger.info("Unable to find repository base packages.  If you need Repositories please define "
					+ "a @ComponentScan annotation or else disable *RepositoriesAutoConfiguration");
		}
		return basePackages;
	}

	/**
	 * The Spring Data annotation used to enable the particular repository support.
	 */
	protected abstract Class<? extends Annotation> getAnnotation();

	/**
	 * The configuration class that will be used by Spring Boot as a template.
	 */
	protected abstract Class<?> getConfiguration();

	/**
	 * The {@link RepositoryConfigurationExtension} for the particular repository support.
	 */
	protected abstract RepositoryConfigurationExtension getRepositoryConfigurationExtension();

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}
