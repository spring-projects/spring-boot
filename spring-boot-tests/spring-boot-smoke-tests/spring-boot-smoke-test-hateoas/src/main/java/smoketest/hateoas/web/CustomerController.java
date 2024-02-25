/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.hateoas.web;

import smoketest.hateoas.domain.Customer;
import smoketest.hateoas.domain.CustomerRepository;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CustomerController class.
 */
@Controller
@RequestMapping("/customers")
@ExposesResourceFor(Customer.class)
public class CustomerController {

	private final CustomerRepository repository;

	private final EntityLinks entityLinks;

	/**
     * Constructs a new CustomerController with the specified CustomerRepository and EntityLinks.
     * 
     * @param repository the CustomerRepository used for accessing customer data
     * @param entityLinks the EntityLinks used for generating links to related entities
     */
    public CustomerController(CustomerRepository repository, EntityLinks entityLinks) {
		this.repository = repository;
		this.entityLinks = entityLinks;
	}

	/**
     * Retrieves a list of customers.
     * 
     * @return The HTTP entity containing the collection of customers.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	HttpEntity<CollectionModel<Customer>> showCustomers() {
		CollectionModel<Customer> resources = CollectionModel.of(this.repository.findAll());
		resources.add(this.entityLinks.linkToCollectionResource(Customer.class));
		return new ResponseEntity<>(resources, HttpStatus.OK);
	}

	/**
     * Retrieves a specific customer by their ID.
     * 
     * @param id the ID of the customer to retrieve
     * @return the HTTP entity containing the customer resource
     */
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	HttpEntity<EntityModel<Customer>> showCustomer(@PathVariable Long id) {
		EntityModel<Customer> resource = EntityModel.of(this.repository.findOne(id));
		resource.add(this.entityLinks.linkToItemResource(Customer.class, id));
		return new ResponseEntity<>(resource, HttpStatus.OK);
	}

}
