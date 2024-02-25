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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Neo4j reactive
 * Repositories.
 *
 * @author Michael J. Simons
 */
class Neo4jReactiveRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	/**
	 * Returns the annotation class {@code EnableReactiveNeo4jRepositories} that is used
	 * to enable reactive Neo4j repositories.
	 * @return the annotation class {@code EnableReactiveNeo4jRepositories}
	 */
	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableReactiveNeo4jRepositories.class;
	}

	/**
	 * Returns the configuration class for enabling reactive Neo4j repositories.
	 * @return the configuration class for enabling reactive Neo4j repositories
	 */
	@Override
	protected Class<?> getConfiguration() {
		return EnableReactiveNeo4jRepositoriesConfiguration.class;
	}

	/**
	 * Returns the repository configuration extension for ReactiveNeo4j repositories.
	 * @return the ReactiveNeo4jRepositoryConfigurationExtension
	 */
	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new ReactiveNeo4jRepositoryConfigurationExtension();
	}

	/**
	 * EnableReactiveNeo4jRepositoriesConfiguration class.
	 */
	@EnableReactiveNeo4jRepositories
	private static final class EnableReactiveNeo4jRepositoriesConfiguration {

	}

}
