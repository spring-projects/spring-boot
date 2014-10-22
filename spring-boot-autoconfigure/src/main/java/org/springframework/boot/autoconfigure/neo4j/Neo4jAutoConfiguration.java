package org.springframework.boot.autoconfigure.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for Neo4j.
 *
 * @author Amer Aljovic
 */
@Configuration
@ConditionalOnClass(GraphDatabase.class)
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jAutoConfiguration
{
    @Autowired
    private Neo4jProperties props;

    @Bean
    GraphDatabaseService graphDatabaseService() {
        if (isNotEmpty(props.getUri())) {
            if (isNotEmpty(props.getUsername()) && isNotEmpty(props.getPassword())) {
                return new SpringRestGraphDatabase(
                        props.getUri(), props.getUsername(), props.getPassword());
            }
            return new SpringRestGraphDatabase(props.getUri(), props.getUsername(), props.getPassword());
        }
        return new GraphDatabaseFactory().newEmbeddedDatabase(props.getLocalPath());
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
