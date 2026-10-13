/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.mongodb.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.Repository

interface CityRepository :
	Repository<City, Long> {
	fun findAll(pageable: Pageable?): Page<City>
	fun findByNameAndStateAllIgnoringCase(name: String, state: String): City?
}
