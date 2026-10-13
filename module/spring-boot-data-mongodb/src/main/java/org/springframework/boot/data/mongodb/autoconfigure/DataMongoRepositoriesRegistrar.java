/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.mongodb.repository.config.MongoRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Mongo
 * Repositories.
 *
 * @author Dave Syer
 */
class DataMongoRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMongoRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableMongoRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new MongoRepositoryConfigurationExtension();
	}

	@EnableMongoRepositories
	private static final class EnableMongoRepositoriesConfiguration {

	}

}
