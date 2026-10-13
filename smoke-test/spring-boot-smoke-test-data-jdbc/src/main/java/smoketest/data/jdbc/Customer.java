/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jdbc;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;

public class Customer {

	@Id
	private @Nullable Long id;

	private @Nullable String firstName;

	private @Nullable LocalDate dateOfBirth;

	public @Nullable Long getId() {
		return this.id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	public @Nullable String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	public @Nullable LocalDate getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

}
