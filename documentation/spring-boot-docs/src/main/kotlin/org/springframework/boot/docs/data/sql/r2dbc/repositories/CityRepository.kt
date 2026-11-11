/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.r2dbc.repositories

import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono

interface CityRepository : Repository<City, Long> {

	fun findByNameAndStateAllIgnoringCase(name: String, state: String): Mono<City>

}

