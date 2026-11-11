/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.domain.city;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ReactiveCityRepository extends ReactiveCrudRepository<City, String> {

}
