/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.cassandra;

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
class CassandraRepositoriesAutoConfigureRegistrar extends AbstractRepositoryConfigurationSourceSupport {

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
	private static class EnableCassandraRepositoriesConfiguration {

	}

}
