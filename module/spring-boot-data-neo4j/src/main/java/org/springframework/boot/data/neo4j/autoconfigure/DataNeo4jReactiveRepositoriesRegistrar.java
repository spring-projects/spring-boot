/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.autoconfigure;

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
class DataNeo4jReactiveRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableReactiveNeo4jRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableReactiveNeo4jRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new ReactiveNeo4jRepositoryConfigurationExtension();
	}

	@EnableReactiveNeo4jRepositories
	private static final class EnableReactiveNeo4jRepositoriesConfiguration {

	}

}
