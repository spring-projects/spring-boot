/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.test.autoconfigure;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Example graph used with {@link DataNeo4jTest @DataNeo4jTest} tests.
 *
 * @author Eddú Meléndez
 */
@Node
public class ExampleGraph {

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

	@Property
	private String description;

	public ExampleGraph(String description) {
		this.description = description;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
