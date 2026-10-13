/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.country;

import java.io.Serializable;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class Country implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

	private final String name;

	public Country(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
