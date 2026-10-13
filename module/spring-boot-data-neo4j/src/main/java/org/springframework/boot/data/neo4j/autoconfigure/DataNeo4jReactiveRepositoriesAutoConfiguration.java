/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.autoconfigure;

import org.neo4j.driver.Driver;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.data.neo4j.repository.support.ReactiveNeo4jRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j Reactive
 * Repositories.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = DataNeo4jReactiveAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, ReactiveNeo4jRepository.class, Flux.class })
@ConditionalOnMissingBean({ ReactiveNeo4jRepositoryFactoryBean.class,
		ReactiveNeo4jRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.REACTIVE)
@Import(DataNeo4jReactiveRepositoriesRegistrar.class)
public final class DataNeo4jReactiveRepositoriesAutoConfiguration {

}
