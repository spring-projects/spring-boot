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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.elasticsearch.repository.config.ElasticsearchRepositoryConfigExtension;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Elasticsearch
 * Repositories.
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 */
class ElasticsearchRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	/**
     * Returns the annotation class that is used to enable Elasticsearch repositories.
     *
     * @return the annotation class {@code EnableElasticsearchRepositories}
     */
    @Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableElasticsearchRepositories.class;
	}

	/**
     * Returns the configuration class for enabling Elasticsearch repositories.
     * 
     * @return the configuration class for enabling Elasticsearch repositories
     */
    @Override
	protected Class<?> getConfiguration() {
		return EnableElasticsearchRepositoriesConfiguration.class;
	}

	/**
     * Returns the repository configuration extension for Elasticsearch repositories.
     * 
     * @return the ElasticsearchRepositoryConfigExtension instance
     */
    @Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new ElasticsearchRepositoryConfigExtension();
	}

	/**
     * EnableElasticsearchRepositoriesConfiguration class.
     */
    @EnableElasticsearchRepositories
	private static final class EnableElasticsearchRepositoriesConfiguration {

	}

}
