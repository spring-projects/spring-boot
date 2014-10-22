package org.springframework.boot.autoconfigure.data.neo4j;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.GraphRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j
 * Repositories.
 * <p>
 * Activates when there is no bean of type
 * {@link org.springframework.data.neo4j.repository.GraphRepositoryFactoryBean}
 * configured in the context, the Spring Data Graph
 * {@link org.springframework.data.neo4j.repository.GraphRepository} type is on the
 * classpath, the GraphDatabase is on the classpath, and there is no other
 * configured {@link org.springframework.data.neo4j.repository.GraphRepository}.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling Mongo repositories
 * using the
 * {@link org.springframework.data.neo4j.config.EnableNeo4jRepositories}
 * annotation.
 *
 * @author Amer Aljovic
 * @see EnableNeo4jRepositories
 */
@Configuration
@ConditionalOnClass({GraphDatabase.class, GraphRepository.class})
@ConditionalOnMissingBean (GraphRepositoryFactoryBean.class)
@ConditionalOnExpression ("${spring.data.neo4j.repositories.enabled:true}")
@Import (Neo4jRepositoriesAutoConfigureRegistrar.class)
@AutoConfigureAfter (Neo4jAutoConfiguration.class)
public class Neo4jRepositoriesAutoConfiguration {
}
