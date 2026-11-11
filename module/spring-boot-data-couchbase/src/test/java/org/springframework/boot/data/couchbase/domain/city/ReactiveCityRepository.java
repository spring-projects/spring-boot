/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.domain.city;

import reactor.core.publisher.Mono;

import org.springframework.data.repository.Repository;

public interface ReactiveCityRepository extends Repository<City, Long> {

	Mono<City> save(City city);

	Mono<City> findById(Long id);

}
