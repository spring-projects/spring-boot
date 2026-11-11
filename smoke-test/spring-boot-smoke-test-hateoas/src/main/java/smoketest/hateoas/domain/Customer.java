/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.hateoas.domain;

public class Customer {

	private final Long id;

	private final String firstName;

	private final String lastName;

	public Customer(Long id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public Long getId() {
		return this.id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

}
