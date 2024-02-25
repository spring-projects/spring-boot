/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.hateoas.domain;

/**
 * Customer class.
 */
public class Customer {

	private final Long id;

	private final String firstName;

	private final String lastName;

	/**
     * Constructs a new Customer object with the specified id, first name, and last name.
     *
     * @param id the unique identifier for the customer
     * @param firstName the first name of the customer
     * @param lastName the last name of the customer
     */
    public Customer(Long id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	/**
     * Returns the ID of the customer.
     *
     * @return the ID of the customer
     */
    public Long getId() {
		return this.id;
	}

	/**
     * Returns the first name of the customer.
     *
     * @return the first name of the customer
     */
    public String getFirstName() {
		return this.firstName;
	}

	/**
     * Returns the last name of the customer.
     *
     * @return the last name of the customer
     */
    public String getLastName() {
		return this.lastName;
	}

}
