/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import org.jspecify.annotations.Nullable;

/**
 * Example entity used with {@link JdbcTest @JdbcTest} tests.
 *
 * @author Stephane Nicoll
 */
public class ExampleEntity {

	private final int id;

	private @Nullable String name;

	public ExampleEntity(int id, @Nullable String name) {
		this.id = id;
		this.name = name;
	}

	public ExampleEntity(int id) {
		this(id, null);
	}

	public int getId() {
		return this.id;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

}
