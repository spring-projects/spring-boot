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

package org.springframework.boot.autoconfigure.data.r2dbc;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.config.R2dbcRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data R2DBC
 * Repositories.
 *
 * @author Mark Paluch
 */
class R2dbcRepositoriesAutoConfigureRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	/**
     * Returns the annotation class that is used to enable R2dbc repositories.
     *
     * @return the annotation class {@code EnableR2dbcRepositories}
     */
    @Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableR2dbcRepositories.class;
	}

	/**
     * Returns the configuration class for enabling R2DBC repositories.
     * 
     * @return the configuration class for enabling R2DBC repositories
     */
    @Override
	protected Class<?> getConfiguration() {
		return EnableR2dbcRepositoriesConfiguration.class;
	}

	/**
     * Returns the repository configuration extension for R2DBC repositories.
     * 
     * @return the repository configuration extension
     */
    @Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new R2dbcRepositoryConfigurationExtension();
	}

	/**
     * EnableR2dbcRepositoriesConfiguration class.
     */
    @EnableR2dbcRepositories
	private static final class EnableR2dbcRepositoriesConfiguration {

	}

}
