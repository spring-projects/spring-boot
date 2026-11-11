/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.cassandra.repository.config.CassandraRepositoryConfigurationExtension;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Cassandra
 * Repositories.
 *
 * @author Eddú Meléndez
 */
class DataCassandraRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableCassandraRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableCassandraRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new CassandraRepositoryConfigurationExtension();
	}

	@EnableCassandraRepositories
	private static final class EnableCassandraRepositoriesConfiguration {

	}

}
