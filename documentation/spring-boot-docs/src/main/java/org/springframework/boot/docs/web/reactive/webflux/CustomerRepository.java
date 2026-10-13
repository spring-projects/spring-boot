/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux;

import reactor.core.publisher.Flux;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {

	Flux<Customer> findByUser(User user);

}
