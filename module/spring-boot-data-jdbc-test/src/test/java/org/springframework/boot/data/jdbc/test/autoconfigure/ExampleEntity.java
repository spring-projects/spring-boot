/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jdbc.test.autoconfigure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Example entity used with {@link DataJdbcTest @DataJdbcTest} tests.
 *
 * @author Andy Wilkinson
 */
@Table("EXAMPLE_ENTITY")
public class ExampleEntity {

	@Id
	@SuppressWarnings("NullAway.Init")
	private Long id;

	private final String name;

	private final String reference;

	public ExampleEntity(String name, String reference) {
		this.name = name;
		this.reference = reference;
	}

	public String getName() {
		return this.name;
	}

	public String getReference() {
		return this.reference;
	}

}
