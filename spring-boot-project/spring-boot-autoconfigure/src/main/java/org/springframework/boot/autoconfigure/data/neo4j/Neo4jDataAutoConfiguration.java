/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Set;

import org.neo4j.driver.Driver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * @since 1.4.0
 */
@AutoConfiguration(before = TransactionAutoConfiguration.class,
		after = { Neo4jAutoConfiguration.class, TransactionManagerCustomizationAutoConfiguration.class })
@ConditionalOnClass({ Driver.class, Neo4jTransactionManager.class, PlatformTransactionManager.class })
@EnableConfigurationProperties(Neo4jDataProperties.class)
@ConditionalOnBean(Driver.class)
public class Neo4jDataAutoConfiguration {

	/**
     * Creates a new instance of Neo4jConversions if no other bean of the same type is present.
     * 
     * @return the Neo4jConversions bean
     */
    @Bean
	@ConditionalOnMissingBean
	public Neo4jConversions neo4jConversions() {
		return new Neo4jConversions();
	}

	/**
     * Returns the Neo4jManagedTypes bean if it is missing in the application context.
     * This bean is responsible for scanning the application context and identifying the initial entity classes.
     * The initial entity classes are determined by scanning for classes annotated with @Node or @RelationshipProperties.
     * 
     * @param applicationContext the application context
     * @return the Neo4jManagedTypes bean
     * @throws ClassNotFoundException if a class cannot be found during the scanning process
     */
    @Bean
	@ConditionalOnMissingBean
	Neo4jManagedTypes neo4jManagedTypes(ApplicationContext applicationContext) throws ClassNotFoundException {
		Set<Class<?>> initialEntityClasses = new EntityScanner(applicationContext).scan(Node.class,
				RelationshipProperties.class);
		return Neo4jManagedTypes.fromIterable(initialEntityClasses);
	}

	/**
     * Creates a new instance of {@link Neo4jMappingContext} if no other bean of the same type is present.
     * 
     * @param managedTypes The managed types for the mapping context.
     * @param neo4jConversions The Neo4j conversions to be used by the mapping context.
     * @return The created {@link Neo4jMappingContext} instance.
     */
    @Bean
	@ConditionalOnMissingBean
	public Neo4jMappingContext neo4jMappingContext(Neo4jManagedTypes managedTypes, Neo4jConversions neo4jConversions) {
		Neo4jMappingContext context = new Neo4jMappingContext(neo4jConversions);
		context.setManagedTypes(managedTypes);
		return context;
	}

	/**
     * Creates a {@link DatabaseSelectionProvider} bean if no other bean of the same type is present.
     * The bean is created based on the provided {@link Neo4jDataProperties} instance.
     * If the database property is not null, a {@link DatabaseSelectionProvider} is created using the provided database name.
     * Otherwise, the default {@link DatabaseSelectionProvider} is returned.
     *
     * @param properties the {@link Neo4jDataProperties} instance used to determine the database name
     * @return the created {@link DatabaseSelectionProvider} bean
     */
    @Bean
	@ConditionalOnMissingBean
	public DatabaseSelectionProvider databaseSelectionProvider(Neo4jDataProperties properties) {
		String database = properties.getDatabase();
		return (database != null) ? DatabaseSelectionProvider.createStaticDatabaseSelectionProvider(database)
				: DatabaseSelectionProvider.getDefaultSelectionProvider();
	}

	/**
     * Creates a Neo4jClient bean using the provided driver and databaseNameProvider.
     * 
     * @param driver The Neo4j driver to be used by the client.
     * @param databaseNameProvider The provider for selecting the database to be used by the client.
     * @return The created Neo4jClient bean.
     */
    @Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	public Neo4jClient neo4jClient(Driver driver, DatabaseSelectionProvider databaseNameProvider) {
		return Neo4jClient.create(driver, databaseNameProvider);
	}

	/**
     * Creates a new instance of Neo4jTemplate using the provided Neo4jClient and Neo4jMappingContext.
     * 
     * @param neo4jClient The Neo4jClient to be used by the Neo4jTemplate.
     * @param neo4jMappingContext The Neo4jMappingContext to be used by the Neo4jTemplate.
     * @return A new instance of Neo4jTemplate.
     */
    @Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(Neo4jOperations.class)
	public Neo4jTemplate neo4jTemplate(Neo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		return new Neo4jTemplate(neo4jClient, neo4jMappingContext);
	}

	/**
     * Creates a Neo4jTransactionManager bean if no other TransactionManager bean is present.
     * 
     * @param driver The Neo4j Driver used for database connections.
     * @param databaseNameProvider The provider for selecting the database name.
     * @param optionalCustomizers Optional customizers for the TransactionManager.
     * @return The Neo4jTransactionManager bean.
     */
    @Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	@ConditionalOnMissingBean(TransactionManager.class)
	public Neo4jTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider,
			ObjectProvider<TransactionManagerCustomizers> optionalCustomizers) {
		Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(driver, databaseNameProvider);
		optionalCustomizers.ifAvailable((customizer) -> customizer.customize((TransactionManager) transactionManager));
		return transactionManager;
	}

}
