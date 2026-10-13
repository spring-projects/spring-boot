/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.test.autoconfigure;

import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataNeo4jTest @DataNeo4jTest} tests.
 *
 * @author Eddú Meléndez
 * @author Michael J. Simons
 */
@Service
public class ExampleService {

	private final Neo4jTemplate neo4jTemplate;

	public ExampleService(Neo4jTemplate neo4jTemplate) {
		this.neo4jTemplate = neo4jTemplate;
	}

	public boolean hasNode(Class<?> type) {
		return this.neo4jTemplate.count(type) == 1;
	}

}
