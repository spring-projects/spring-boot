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

package org.springframework.boot.autoconfigure.data;

import java.lang.annotation.Annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
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
 */
public abstract class AbstractRepositoryConfigurationSourceSupport
		implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware,
		EnvironmentAware {

	private ResourceLoader resourceLoader;

	private BeanFactory beanFactory;

	private Environment environment;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		new RepositoryConfigurationDelegate(getConfigurationSource(registry),
				this.resourceLoader, this.environment).registerRepositoriesIn(registry,
						getRepositoryConfigurationExtension());
	}

	private AnnotationRepositoryConfigurationSource getConfigurationSource(
			BeanDefinitionRegistry beanDefinitionRegistry) {
		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				getConfiguration(), true);
		return new AnnotationRepositoryConfigurationSource(metadata, getAnnotation(),
				this.resourceLoader, this.environment, beanDefinitionRegistry) {
			@Override
			public Streamable<String> getBasePackages() {
				return AbstractRepositoryConfigurationSourceSupport.this
						.getBasePackages();
			}

			@Override
			public BootstrapMode getBootstrapMode() {
				return AbstractRepositoryConfigurationSourceSupport.this
						.getBootstrapMode();
			}

		};
	}

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

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
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
