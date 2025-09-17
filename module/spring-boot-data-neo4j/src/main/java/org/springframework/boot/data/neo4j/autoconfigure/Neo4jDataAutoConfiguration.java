/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.neo4j.autoconfigure;

import java.util.Set;

import org.neo4j.driver.Driver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScanner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.aot.Neo4jManagedTypes;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Neo4j.
 *
 * @author Michael Hunger
 * @author Josh Long
 * @author Vince Bickers
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Michael J. Simons
 * @since 4.0.0
 */
@AutoConfiguration(before = TransactionAutoConfiguration.class,
		after = { Neo4jAutoConfiguration.class, TransactionManagerCustomizationAutoConfiguration.class })
@ConditionalOnClass({ Driver.class, Neo4jTransactionManager.class, PlatformTransactionManager.class })
@EnableConfigurationProperties(DataNeo4jProperties.class)
@ConditionalOnBean(Driver.class)
public final class Neo4jDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Neo4jConversions neo4jConversions() {
		return new Neo4jConversions();
	}

	@Bean
	@ConditionalOnMissingBean
	Neo4jManagedTypes neo4jManagedTypes(ApplicationContext applicationContext) throws ClassNotFoundException {
		Set<Class<?>> initialEntityClasses = new EntityScanner(applicationContext).scan(Node.class,
				RelationshipProperties.class);
		return Neo4jManagedTypes.fromIterable(initialEntityClasses);
	}

	@Bean
	@ConditionalOnMissingBean
	Neo4jMappingContext neo4jMappingContext(Neo4jManagedTypes managedTypes, Neo4jConversions neo4jConversions) {
		Neo4jMappingContext context = new Neo4jMappingContext(neo4jConversions);
		context.setManagedTypes(managedTypes);
		return context;
	}

	@Bean
	@ConditionalOnMissingBean
	DatabaseSelectionProvider databaseSelectionProvider(DataNeo4jProperties properties) {
		String database = properties.getDatabase();
		return (database != null) ? DatabaseSelectionProvider.createStaticDatabaseSelectionProvider(database)
				: DatabaseSelectionProvider.getDefaultSelectionProvider();
	}

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

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	@ConditionalOnMissingBean(TransactionManager.class)
	Neo4jTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider,
			ObjectProvider<TransactionManagerCustomizers> optionalCustomizers) {
		Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(driver, databaseNameProvider);
		optionalCustomizers.ifAvailable((customizer) -> customizer.customize(transactionManager));
		return transactionManager;
	}

}
