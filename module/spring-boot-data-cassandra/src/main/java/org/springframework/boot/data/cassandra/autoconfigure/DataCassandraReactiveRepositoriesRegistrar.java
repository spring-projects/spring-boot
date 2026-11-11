/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;
import org.springframework.data.cassandra.repository.config.ReactiveCassandraRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Cassandra
 * Reactive Repositories.
 *
 * @author Eddú Meléndez
 */
class DataCassandraReactiveRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableReactiveCassandraRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableReactiveCassandraRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new ReactiveCassandraRepositoryConfigurationExtension();
	}

	@EnableReactiveCassandraRepositories
	private static final class EnableReactiveCassandraRepositoriesConfiguration {

	}

}
