/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.test.autoconfigure;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

/**
 * Example reactive repository used with {@link DataNeo4jTest @DataNeo4jTest} tests.
 *
 * @author Stephane Nicoll
 */
interface ExampleReactiveRepository extends ReactiveNeo4jRepository<ExampleGraph, Long> {

}
