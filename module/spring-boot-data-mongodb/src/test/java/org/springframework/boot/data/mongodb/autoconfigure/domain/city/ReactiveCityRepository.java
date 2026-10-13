/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure.domain.city;

import reactor.core.publisher.Flux;

import org.springframework.data.repository.Repository;

public interface ReactiveCityRepository extends Repository<City, Long> {

	Flux<City> findAll();

}
