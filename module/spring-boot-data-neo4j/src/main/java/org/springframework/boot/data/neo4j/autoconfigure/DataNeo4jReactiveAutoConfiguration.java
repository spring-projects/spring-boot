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

import org.neo4j.driver.Driver;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * @since 4.0.0
 */
@AutoConfiguration(after = DataNeo4jAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, ReactiveNeo4jTemplate.class, ReactiveTransactionManager.class, Flux.class })
@ConditionalOnBean(Driver.class)
@EnableConfigurationProperties(DataNeo4jProperties.class)
public final class DataNeo4jReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ReactiveDatabaseSelectionProvider reactiveDatabaseSelectionProvider(DataNeo4jProperties properties) {
		String database = properties.getDatabase();
		return (database != null) ? ReactiveDatabaseSelectionProvider.createStaticDatabaseSelectionProvider(database)
				: ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider();
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	ReactiveNeo4jClient reactiveNeo4jClient(Driver driver, ReactiveDatabaseSelectionProvider databaseNameProvider) {
		return ReactiveNeo4jClient.create(driver, databaseNameProvider);
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(ReactiveNeo4jOperations.class)
	@ConditionalOnBean(Neo4jMappingContext.class)
	ReactiveNeo4jTemplate reactiveNeo4jTemplate(ReactiveNeo4jClient neo4jClient,
			Neo4jMappingContext neo4jMappingContext) {
		return new ReactiveNeo4jTemplate(neo4jClient, neo4jMappingContext);
	}

}
