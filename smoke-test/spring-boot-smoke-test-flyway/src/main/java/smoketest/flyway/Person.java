/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.flyway;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.jspecify.annotations.Nullable;

@Entity
public class Person {

	@Id
	@SequenceGenerator(name = "person_generator", sequenceName = "person_sequence", allocationSize = 1)
	@GeneratedValue(generator = "person_generator")
	private @Nullable Long id;

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastname) {
		this.lastName = lastname;
	}

	@Override
	public String toString() {
		return "Person [firstName=" + this.firstName + ", lastName=" + this.lastName + "]";
	}

}
