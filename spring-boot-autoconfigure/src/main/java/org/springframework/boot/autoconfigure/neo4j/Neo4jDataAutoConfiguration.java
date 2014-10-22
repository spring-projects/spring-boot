package org.springframework.boot.autoconfigure.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j support.
 * <p>
 * Extends Neo4jConfiguration which already auto-configures Spring Data Neo4j.
 * The base package is set to root by default.
 *
 * @author Amer Aljovic
 */
@Configuration
@ConditionalOnClass(GraphDatabaseService.class)
@AutoConfigureAfter(Neo4jAutoConfiguration.class)
public class Neo4jDataAutoConfiguration extends Neo4jConfiguration
{
    public Neo4jDataAutoConfiguration()
    {
        setBasePackage("");
    }
}
