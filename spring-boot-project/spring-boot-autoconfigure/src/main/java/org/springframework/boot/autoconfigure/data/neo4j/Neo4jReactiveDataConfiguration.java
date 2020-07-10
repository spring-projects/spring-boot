/*
 * Copyright 2012-2020 the original author or authors.
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
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.config.Neo4jDefaultReactiveCallbacksRegistrar;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Internal configuration for the reactive Neo4j client.
 *
 * @author Michael J. Simons
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveNeo4jTransactionManager.class, ReactiveTransactionManager.class, Flux.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.REACTIVE)
@AutoConfigureAfter(Neo4jAutoConfiguration.class)
@AutoConfigureBefore(Neo4jReactiveRepositoriesConfiguration.class)
@Import(Neo4jDefaultReactiveCallbacksRegistrar.class)
class Neo4jReactiveDataConfiguration {

	@Bean("reactiveDatabaseSelectionProvider")
	@ConditionalOnProperty(prefix = "spring.data.neo4j", name = "database")
	@ConditionalOnMissingBean
	@Order(-30)
	ReactiveDatabaseSelectionProvider staticDatabaseSelectionProvider(Neo4jDataProperties dataProperties) {
		return ReactiveDatabaseSelectionProvider.createStaticDatabaseSelectionProvider(dataProperties.getDatabase());
	}

	@Bean("reactiveDatabaseSelectionProvider")
	@ConditionalOnMissingBean
	@Order(-20)
	ReactiveDatabaseSelectionProvider defaultSelectionProvider() {
		return ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider();
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	ReactiveNeo4jClient neo4jClient(Driver driver) {
		return ReactiveNeo4jClient.create(driver);
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(ReactiveNeo4jOperations.class)
	ReactiveNeo4jTemplate neo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			ReactiveDatabaseSelectionProvider databaseNameProvider) {
		return new ReactiveNeo4jTemplate(neo4jClient, neo4jMappingContext, databaseNameProvider);
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	@ConditionalOnMissingBean(ReactiveTransactionManager.class)
	ReactiveTransactionManager transactionManager(Driver driver,
			ReactiveDatabaseSelectionProvider databaseNameProvider) {
		return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider);
	}

}
