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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;

/**
 * InMemoryCustomerRepository class.
 */
@Repository
public class InMemoryCustomerRepository implements CustomerRepository {

	private final List<Customer> customers = new ArrayList<>();

	/**
	 * Constructs a new InMemoryCustomerRepository and initializes it with three
	 * customers. The customers are added to the repository with their respective IDs,
	 * first names, and last names.
	 *
	 * The first customer has an ID of 1, first name "Oliver", and last name "Gierke". The
	 * second customer has an ID of 2, first name "Andy", and last name "Wilkinson". The
	 * third customer has an ID of 2, first name "Dave", and last name "Syer".
	 */
	public InMemoryCustomerRepository() {
		this.customers.add(new Customer(1L, "Oliver", "Gierke"));
		this.customers.add(new Customer(2L, "Andy", "Wilkinson"));
		this.customers.add(new Customer(2L, "Dave", "Syer"));
	}

	/**
	 * Retrieves all customers from the repository.
	 * @return a list of Customer objects representing all customers in the repository.
	 */
	@Override
	public List<Customer> findAll() {
		return this.customers;
	}

	/**
	 * Retrieves a customer with the specified ID from the repository.
	 * @param id the ID of the customer to retrieve
	 * @return the customer with the specified ID, or null if not found
	 */
	@Override
	public Customer findOne(Long id) {
		for (Customer customer : this.customers) {
			if (ObjectUtils.nullSafeEquals(customer.getId(), id)) {
				return customer;
			}
		}
		return null;
	}

}
