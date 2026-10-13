/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.r2dbc.domain.city;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CityRepository extends ReactiveCrudRepository<City, Long> {

}
