/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.reactive.webflux

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface CustomerRepository : ReactiveCrudRepository<Customer, Long> {

	fun findByUser(user: User?): Flux<Customer>

}

