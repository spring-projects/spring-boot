/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.flyway;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * Person class.
 */
@Entity
public class Person {

	@Id
	@SequenceGenerator(name = "person_generator", sequenceName = "person_sequence", allocationSize = 1)
	@GeneratedValue(generator = "person_generator")
	private Long id;

	private String firstName;

	private String lastName;

	/**
	 * Returns the first name of the person.
	 * @return the first name of the person
	 */
	public String getFirstName() {
		return this.firstName;
	}

	/**
	 * Sets the first name of the person.
	 * @param firstName the first name to be set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Returns the last name of the person.
	 * @return the last name of the person
	 */
	public String getLastName() {
		return this.lastName;
	}

	/**
	 * Sets the last name of the person.
	 * @param lastname the last name to be set
	 */
	public void setLastName(String lastname) {
		this.lastName = lastname;
	}

	/**
	 * Returns a string representation of the Person object.
	 * @return a string representation of the Person object
	 */
	@Override
	public String toString() {
		return "Person [firstName=" + this.firstName + ", lastName=" + this.lastName + "]";
	}

}
