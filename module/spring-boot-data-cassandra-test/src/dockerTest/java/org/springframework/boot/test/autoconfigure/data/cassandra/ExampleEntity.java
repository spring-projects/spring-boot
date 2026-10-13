/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.data.cassandra;

import org.springframework.boot.data.cassandra.test.autoconfigure.DataCassandraTest;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Example graph used with {@link DataCassandraTest @DataCassandraTest} tests.
 *
 * @author Artsiom Yudovin
 */
@Table
public class ExampleEntity {

	@PrimaryKey
	@SuppressWarnings("NullAway.Init")
	private String id;

	@SuppressWarnings("NullAway.Init")
	private String description;

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
