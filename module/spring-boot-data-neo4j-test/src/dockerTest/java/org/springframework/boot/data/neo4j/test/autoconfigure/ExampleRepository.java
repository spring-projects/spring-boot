/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.test.autoconfigure;

import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * Example repository used with {@link DataNeo4jTest @DataNeo4jTest} tests.
 *
 * @author Eddú Meléndez
 */
interface ExampleRepository extends Neo4jRepository<ExampleGraph, Long> {

}
