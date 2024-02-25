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

package org.springframework.boot.autoconfigure.data.couchbase;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.couchbase.repository.config.CouchbaseRepositoryConfigurationExtension;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data Couchbase
 * Repositories.
 *
 * @author Eddú Meléndez
 */
class CouchbaseRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	/**
	 * Returns the annotation class that is used to enable Couchbase repositories.
	 * @return the annotation class {@code EnableCouchbaseRepositories}
	 */
	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableCouchbaseRepositories.class;
	}

	/**
	 * Returns the configuration class for enabling Couchbase repositories.
	 * @return the configuration class for enabling Couchbase repositories
	 */
	@Override
	protected Class<?> getConfiguration() {
		return EnableCouchbaseRepositoriesConfiguration.class;
	}

	/**
	 * Returns the repository configuration extension for Couchbase repositories.
	 * @return The CouchbaseRepositoryConfigurationExtension instance.
	 */
	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new CouchbaseRepositoryConfigurationExtension();
	}

	/**
	 * EnableCouchbaseRepositoriesConfiguration class.
	 */
	@EnableCouchbaseRepositories
	private static final class EnableCouchbaseRepositoriesConfiguration {

	}

}
