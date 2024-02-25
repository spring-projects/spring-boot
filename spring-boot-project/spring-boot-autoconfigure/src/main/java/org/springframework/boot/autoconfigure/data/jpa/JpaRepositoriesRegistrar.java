/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.jpa;

import java.lang.annotation.Annotation;
import java.util.Locale;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data JPA
 * Repositories.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Scott Frederick
 */
class JpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	private BootstrapMode bootstrapMode = null;

	/**
     * Returns the annotation class that is used to enable JPA repositories.
     *
     * @return the annotation class {@code EnableJpaRepositories}
     */
    @Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJpaRepositories.class;
	}

	/**
     * Returns the configuration class for enabling JPA repositories.
     * 
     * @return the configuration class for enabling JPA repositories
     */
    @Override
	protected Class<?> getConfiguration() {
		return EnableJpaRepositoriesConfiguration.class;
	}

	/**
     * Returns the repository configuration extension for JPA repositories.
     * 
     * @return the repository configuration extension
     */
    @Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new JpaRepositoryConfigExtension();
	}

	/**
     * Returns the bootstrap mode for the JpaRepositoriesRegistrar.
     * 
     * @return the bootstrap mode, or {@link BootstrapMode#DEFAULT} if not set
     */
    @Override
	protected BootstrapMode getBootstrapMode() {
		return (this.bootstrapMode == null) ? BootstrapMode.DEFAULT : this.bootstrapMode;
	}

	/**
     * Set the environment for this registrar.
     * 
     * @param environment the environment to set
     */
    @Override
	public void setEnvironment(Environment environment) {
		super.setEnvironment(environment);
		configureBootstrapMode(environment);
	}

	/**
     * Configures the bootstrap mode for JPA repositories.
     * 
     * @param environment the environment containing the properties
     */
    private void configureBootstrapMode(Environment environment) {
		String property = environment.getProperty("spring.data.jpa.repositories.bootstrap-mode");
		if (StringUtils.hasText(property)) {
			this.bootstrapMode = BootstrapMode.valueOf(property.toUpperCase(Locale.ENGLISH));
		}
	}

	/**
     * EnableJpaRepositoriesConfiguration class.
     */
    @EnableJpaRepositories
	private static final class EnableJpaRepositoriesConfiguration {

	}

}
