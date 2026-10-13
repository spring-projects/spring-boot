/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.autoconfigure;

import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j
 * Repositories.
 * <p>
 * Activates when there is no bean of type {@link Neo4jRepositoryFactoryBean} or
 * {@link Neo4jRepositoryConfigurationExtension} configured in the context, the Spring
 * Data Neo4j {@link Neo4jRepository} type is on the classpath, the Neo4j client driver
 * API is on the classpath, and there is no other configured {@link Neo4jRepository}.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling Neo4j repositories
 * using the {@link EnableNeo4jRepositories @EnableNeo4jRepositories} annotation.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @author Michael J. Simons
 * @since 4.0.0
 * @see EnableNeo4jRepositories
 */
@AutoConfiguration(after = DataNeo4jAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, Neo4jRepository.class })
@ConditionalOnMissingBean({ Neo4jRepositoryFactoryBean.class, Neo4jRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.IMPERATIVE)
@Import(DataNeo4jRepositoriesRegistrar.class)
public final class DataNeo4jRepositoriesAutoConfiguration {

}
