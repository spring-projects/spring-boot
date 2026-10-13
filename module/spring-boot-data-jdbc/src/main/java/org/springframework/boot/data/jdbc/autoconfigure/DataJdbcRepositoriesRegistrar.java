/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data JDBC
 * Repositories.
 *
 * @author Andy Wilkinson
 */
class DataJdbcRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableJdbcRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableJdbcRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new JdbcRepositoryConfigExtension();
	}

	@EnableJdbcRepositories
	private static final class EnableJdbcRepositoriesConfiguration {

	}

}
