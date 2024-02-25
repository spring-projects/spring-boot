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

package smoketest.data.jdbc;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;

/**
 * Customer class.
 */
public class Customer {

	@Id
	private Long id;

	private String firstName;

	private LocalDate dateOfBirth;

	/**
	 * Returns the ID of the customer.
	 * @return the ID of the customer
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the ID of the customer.
	 * @param id the ID to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the first name of the customer.
	 * @return the first name of the customer
	 */
	public String getFirstName() {
		return this.firstName;
	}

	/**
	 * Sets the first name of the customer.
	 * @param firstName the first name to be set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Returns the date of birth of the customer.
	 * @return the date of birth of the customer
	 */
	public LocalDate getDateOfBirth() {
		return this.dateOfBirth;
	}

	/**
	 * Sets the date of birth for the customer.
	 * @param dateOfBirth the date of birth to be set
	 */
	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

}
