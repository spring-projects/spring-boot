/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.autoconfigure;

import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Envers
 * Repositories.
 *
 * @author Stefano Cordio
 */
class EnversRevisionRepositoriesRegistrar extends DataJpaRepositoriesRegistrar {

	@Override
	protected Class<?> getConfiguration() {
		return EnableEnversRepositoriesConfiguration.class;
	}

	@EnableEnversRepositories
	private static final class EnableEnversRepositoriesConfiguration {

	}

}
