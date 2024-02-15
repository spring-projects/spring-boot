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

import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Neo4j components that require a {@link PlatformTransactionManager}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(PlatformTransactionManager.class)
class Neo4jTransactionalComponentsConfiguration {

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	Neo4jClient neo4jClient(Driver driver, DatabaseSelectionProvider databaseNameProvider) {
		return Neo4jClient.create(driver, databaseNameProvider);
	}

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(Neo4jOperations.class)
	Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		return new Neo4jTemplate(neo4jClient, neo4jMappingContext);
	}

}
