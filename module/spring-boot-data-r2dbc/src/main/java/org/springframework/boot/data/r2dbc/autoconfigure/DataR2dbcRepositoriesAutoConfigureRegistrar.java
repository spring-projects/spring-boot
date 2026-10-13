/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.autoconfigure;

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
class DataR2dbcRepositoriesAutoConfigureRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableR2dbcRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableR2dbcRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new R2dbcRepositoryConfigurationExtension();
	}

	@EnableR2dbcRepositories
	private static final class EnableR2dbcRepositoriesConfiguration {

	}

}
