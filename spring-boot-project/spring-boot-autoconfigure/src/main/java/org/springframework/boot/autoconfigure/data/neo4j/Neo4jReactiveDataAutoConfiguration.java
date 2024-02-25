/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's reactive Neo4j
 * support.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @since 2.4.0
 */
@AutoConfiguration(after = Neo4jDataAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, ReactiveNeo4jTemplate.class, ReactiveTransactionManager.class, Flux.class })
@ConditionalOnBean(Driver.class)
public class Neo4jReactiveDataAutoConfiguration {

	/**
     * Creates a ReactiveDatabaseSelectionProvider bean if no other bean of the same type is present.
     * This bean is responsible for providing the database selection for reactive Neo4j data operations.
     * The database selection is determined based on the value of the 'database' property in the Neo4jDataProperties.
     * If the 'database' property is not set, the default selection provider is used.
     *
     * @param dataProperties the Neo4jDataProperties object containing the configuration properties
     * @return the ReactiveDatabaseSelectionProvider bean
     */
    @Bean
	@ConditionalOnMissingBean
	public ReactiveDatabaseSelectionProvider reactiveDatabaseSelectionProvider(Neo4jDataProperties dataProperties) {
		String database = dataProperties.getDatabase();
		return (database != null) ? ReactiveDatabaseSelectionProvider.createStaticDatabaseSelectionProvider(database)
				: ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider();
	}

	/**
     * Creates a ReactiveNeo4jClient bean if no other bean of the same type is present.
     * 
     * @param driver The Neo4j driver used for database connectivity.
     * @param databaseNameProvider The provider for selecting the database to connect to.
     * @return The created ReactiveNeo4jClient bean.
     */
    @Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	public ReactiveNeo4jClient reactiveNeo4jClient(Driver driver,
			ReactiveDatabaseSelectionProvider databaseNameProvider) {
		return ReactiveNeo4jClient.create(driver, databaseNameProvider);
	}

	/**
     * Creates a new instance of ReactiveNeo4jTemplate using the provided ReactiveNeo4jClient and Neo4jMappingContext.
     * 
     * @param neo4jClient The ReactiveNeo4jClient used for executing queries.
     * @param neo4jMappingContext The Neo4jMappingContext used for mapping entities to the database.
     * @return A new instance of ReactiveNeo4jTemplate.
     */
    @Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(ReactiveNeo4jOperations.class)
	public ReactiveNeo4jTemplate reactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient,
			Neo4jMappingContext neo4jMappingContext) {
		return new ReactiveNeo4jTemplate(neo4jClient, neo4jMappingContext);
	}

}
