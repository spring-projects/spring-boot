/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.hateoas.web;

import sample.hateoas.domain.Customer;
import sample.hateoas.domain.CustomerRepository;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customers")
@ExposesResourceFor(Customer.class)
public class CustomerController {

	private final CustomerRepository repository;

	private final EntityLinks entityLinks;

	public CustomerController(CustomerRepository repository, EntityLinks entityLinks) {
		this.repository = repository;
		this.entityLinks = entityLinks;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	HttpEntity<Resources<Customer>> showCustomers() {
		Resources<Customer> resources = new Resources<Customer>(
				this.repository.findAll());
		resources.add(this.entityLinks.linkToCollectionResource(Customer.class));
		return new ResponseEntity<Resources<Customer>>(resources, HttpStatus.OK);
	}

	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	HttpEntity<Resource<Customer>> showCustomer(@PathVariable Long id) {
		Resource<Customer> resource = new Resource<Customer>(this.repository.findOne(id));
		resource.add(this.entityLinks.linkToSingleResource(Customer.class, id));
		return new ResponseEntity<Resource<Customer>>(resource, HttpStatus.OK);
	}

}
