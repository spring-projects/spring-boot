package org.springframework.boot.autoconfigure.neo4j;

import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration}.
 *
 * @author Amer Aljovic
 */
public class Neo4jAutoConfigurationTests {
    private AnnotationConfigApplicationContext context;

    @After
    public void close() {
        if (context != null)
            context.close();
    }

    @Test
    public void testEmbeddedDatabaseConfiguration() {
        context = new AnnotationConfigApplicationContext(
                PropertyPlaceholderAutoConfiguration.class, Neo4jAutoConfiguration.class);

        assertNotNull("GraphDatabaseService created", context.getBean(GraphDatabaseService.class));
        assertTrue("Instance of EmbeddedGraphDatabase",
                context.getBean(GraphDatabaseService.class) instanceof EmbeddedGraphDatabase);
    }

    @Test
    public void testServerDatabaseConfiguration() {
        context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(context, "spring.data.neo4j.uri:http://localhost:7474/db/data");
        context.register(PropertyPlaceholderAutoConfiguration.class, Neo4jAutoConfiguration.class);
        context.refresh();

        assertNotNull("GraphDatabaseService created", context.getBean(GraphDatabaseService.class));
        assertTrue("Instance of RestGraphDatabase",
                context.getBean(GraphDatabaseService.class) instanceof RestGraphDatabase);
    }
}