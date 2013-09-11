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
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
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
 * @author Dave Syer
 */
public abstract class AbstractRepositoryConfigurationSourceSupport implements
		BeanFactoryAware, ImportBeanDefinitionRegistrar, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	private static Log logger = LogFactory
			.getLog(AbstractRepositoryConfigurationSourceSupport.class);

	private BeanFactory beanFactory;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {

		final ResourceLoader resourceLoader = new DefaultResourceLoader();
		final AnnotationRepositoryConfigurationSource configurationSource = getConfigurationSource();
		final RepositoryConfigurationExtension extension = getRepositoryConfigurationExtension();
		extension.registerBeansForRoot(registry, configurationSource);

		final RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
		generator.setBeanClassLoader(this.beanClassLoader);

		Collection<RepositoryConfiguration<AnnotationRepositoryConfigurationSource>> repositoryConfigurations = extension
				.getRepositoryConfigurations(configurationSource, resourceLoader);

		for (final RepositoryConfiguration<AnnotationRepositoryConfigurationSource> repositoryConfiguration : repositoryConfigurations) {
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

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	protected abstract RepositoryConfigurationExtension getRepositoryConfigurationExtension();

	protected abstract AnnotationRepositoryConfigurationSource getConfigurationSource();

	protected AnnotationRepositoryConfigurationSource getConfigurationSource(
			Class<?> annotated, Class<? extends Annotation> annotation) {
		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(annotated,
				true);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
				metadata, annotation) {

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
			logger.warn("Unable to find repository base packages.  If you need Repositories please define "
					+ "a @ComponentScan annotation or else disable *RepositoriesAutoConfiguration");
		}
		return basePackages;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
