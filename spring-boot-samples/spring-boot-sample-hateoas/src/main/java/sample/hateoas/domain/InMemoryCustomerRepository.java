/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.hateoas.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;

@Repository
public class InMemoryCustomerRepository implements CustomerRepository {

	private final List<Customer> customers = new ArrayList<>();

	public InMemoryCustomerRepository() {
		this.customers.add(new Customer(1L, "Oliver", "Gierke"));
		this.customers.add(new Customer(2L, "Andy", "Wilkinson"));
		this.customers.add(new Customer(2L, "Dave", "Syer"));
	}

	@Override
	public List<Customer> findAll() {
		return this.customers;
	}

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
