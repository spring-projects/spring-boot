/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.test.autoconfigure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Example entity used with {@link DataJpaTest @DataJpaTest} tests.
 *
 * @author Phillip Webb
 */
@Entity
public class ExampleEntity {

	@Id
	@GeneratedValue
	@SuppressWarnings("NullAway.Init")
	private Long id;

	@SuppressWarnings("NullAway.Init")
	private String name;

	@SuppressWarnings("NullAway.Init")
	private String reference;

	protected ExampleEntity() {
	}

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
