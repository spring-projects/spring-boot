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

package org.springframework.boot.config.data;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.config.AutoConfigurationUtils;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryBeanDefinitionBuilder;
import org.springframework.data.repository.config.RepositoryBeanNameGenerator;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.util.Assert;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data JPA
 * Repositories.
 * 
 * @author Phillip Webb
 */
class JpaRepositoriesAutoConfigureRegistrar implements ImportBeanDefinitionRegistrar,
		BeanFactoryAware, BeanClassLoaderAware {

	private BeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			final BeanDefinitionRegistry registry) {

		final ResourceLoader resourceLoader = new DefaultResourceLoader();
		final AnnotationRepositoryConfigurationSource configurationSource = getConfigurationSource();
		final RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
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

	private AnnotationRepositoryConfigurationSource getConfigurationSource() {
		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				EnableJpaRepositoriesConfiguration.class, true);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
				metadata, EnableJpaRepositories.class) {

			@Override
			public java.lang.Iterable<String> getBasePackages() {
				return JpaRepositoriesAutoConfigureRegistrar.this.getBasePackages();
			};
		};
		return configurationSource;
	}

	protected Iterable<String> getBasePackages() {
		List<String> basePackages = AutoConfigurationUtils
				.getBasePackages(this.beanFactory);
		Assert.notEmpty(
				basePackages,
				"Unable to find JPA repository base packages, please define "
						+ "a @ComponentScan annotation or disable JpaRepositoriesAutoConfigure");
		return basePackages;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@EnableJpaRepositories
	private static class EnableJpaRepositoriesConfiguration {
	}
}
