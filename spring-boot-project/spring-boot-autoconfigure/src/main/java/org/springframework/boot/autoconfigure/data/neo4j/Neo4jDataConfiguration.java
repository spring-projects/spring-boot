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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.config.Neo4jDefaultCallbacksRegistrar;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Internal configuration of Neo4j client and transaction manager.
 *
 * @author Michael J. Simons
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Neo4jTransactionManager.class, PlatformTransactionManager.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.IMPERATIVE)
@AutoConfigureAfter(Neo4jDriverAutoConfiguration.class)
@AutoConfigureBefore(Neo4jRepositoriesConfiguration.class)
@Import(Neo4jDefaultCallbacksRegistrar.class)
class Neo4jDataConfiguration {

	@Bean("databaseSelectionProvider")
	@ConditionalOnProperty(prefix = "spring.data.neo4j", name = "database")
	@ConditionalOnMissingBean
	@Order(-30)
	DatabaseSelectionProvider staticDatabaseSelectionProvider(Neo4jDataProperties dataProperties) {
		return DatabaseSelectionProvider.createStaticDatabaseSelectionProvider(dataProperties.getDatabase());
	}

	@Bean("databaseSelectionProvider")
	@ConditionalOnMissingBean
	@Order(-20)
	DatabaseSelectionProvider defaultSelectionProvider() {
		return DatabaseSelectionProvider.getDefaultSelectionProvider();
	}

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	Neo4jClient neo4jClient(Driver driver) {
		return Neo4jClient.create(driver);
	}

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(Neo4jOperations.class)
	Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext,
			DatabaseSelectionProvider databaseNameProvider) {
		return new Neo4jTemplate(neo4jClient, neo4jMappingContext, databaseNameProvider);
	}

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	Neo4jTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider,
			ObjectProvider<TransactionManagerCustomizers> optionalCustomizers) {
		Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(driver, databaseNameProvider);
		optionalCustomizers.ifAvailable((customizer) -> customizer.customize(transactionManager));
		return transactionManager;
	}

}
